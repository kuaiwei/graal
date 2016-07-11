#
# commands.py - the GraalVM specific commands
#
# ----------------------------------------------------------------------------------------------------
#
# Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
# ----------------------------------------------------------------------------------------------------

import os
import re
import subprocess

import mx

from mx_unittest import unittest
from mx_sigtest import sigtest
from mx_gate import Task
import mx_gate

_suite = mx.suite('truffle')

def javadoc(args, vm=None):
    """build the Javadoc for all API packages"""
    mx.javadoc(['--unified'] + args)

def build(args, vm=None):
    """build the Java sources"""
    opts2 = mx.build(['--source', '1.7'] + args)
    assert len(opts2.remainder) == 0

def sl(args):
    """run an SL program"""
    vmArgs, slArgs = mx.extract_VM_args(args)
    mx.run_java(vmArgs + ['-cp', mx.classpath(["TRUFFLE_API", "com.oracle.truffle.sl"]), "com.oracle.truffle.sl.SLMain"] + slArgs)

def repl(args):
    """run a simple command line debugger for Truffle-implemented languages on the class path"""
    vmArgs, slArgs = mx.extract_VM_args(args, useDoubleDash=True)
    mx.run_java(vmArgs + ['-cp', mx.classpath(), "com.oracle.truffle.tools.debug.shell.client.SimpleREPLClient"] + slArgs)

def testdownstream(args):
    """test downstream users of the Truffle API"""
    jruby_dir = 'jruby'
    jruby_repo = 'https://github.com/jruby/jruby.git'
    jruby_branch = 'truffle-head'
    git = mx.GitConfig()
    if os.path.exists('jruby'):
        git.run(['git', 'reset', 'HEAD', '--hard'], nonZeroIsFatal=True, cwd=jruby_dir)
        git.pull('jruby')
    else:
        git.clone(jruby_repo, jruby_dir)
        git.run(['git', 'checkout', jruby_branch], nonZeroIsFatal=True, cwd=jruby_dir)
    dev_version = _suite.release_version(snapshotSuffix='SNAPSHOT')
    mx.build([])
    mx.maven_install([])
    subprocess.check_call(['./mvnw', '-q', '-Dtruffle.version=' + dev_version], cwd=jruby_dir)
    subprocess.check_call(['bin/jruby', 'tool/jt.rb', 'test', 'fast'], cwd=jruby_dir)

def _truffle_gate_runner(args, tasks):
    jdk = mx.get_jdk(tag=mx.DEFAULT_JDK_TAG)
    if jdk.javaCompliance < '9':
        with Task('Truffle Javadoc', tasks) as t:
            if t: mx.javadoc(['--unified'])
    with Task('Truffle UnitTests', tasks) as t:
        if t: unittest(['--suite', 'truffle', '--enable-timing', '--verbose', '--fail-fast'])
    with Task('Truffle Signature Tests', tasks) as t:
        if t: sigtest(['--check', 'binary'])

mx_gate.add_gate_runner(_suite, _truffle_gate_runner)

mx.update_commands(_suite, {
    'javadoc' : [javadoc, '[SL args|@VM options]'],
    'sl' : [sl, '[SL args|@VM options]'],
    'repl' : [repl, '[REPL Debugger args|@VM options]'],
    'testdownstream' : [testdownstream, ''],
})

"""
Merges META-INF/truffle/language and META-INF/truffle/instrument files.
This code is tightly coupled with the file format generated by
LanguageRegistrationProcessor and InstrumentRegistrationProcessor.
"""
class TruffleArchiveParticipant:
    PROPERTY_RE = re.compile(r'(language\d+|instrument\d+)(\..+)')

    def _truffle_metainf_file(self, arcname):
        if arcname == 'META-INF/truffle/language':
            return 'language'
        if arcname == 'META-INF/truffle/instrument':
            return 'instrument'
        return None

    def __opened__(self, arc, srcArc, services):
        self.settings = {}
        self.arc = arc

    def __add__(self, arcname, contents):
        metainfFile = self._truffle_metainf_file(arcname)
        if metainfFile:
            propertyRe = TruffleArchiveParticipant.PROPERTY_RE
            properties = {}
            for line in contents.strip().split('\n'):
                if not line.startswith('#'):
                    m = propertyRe.match(line)
                    assert m, 'line in ' + arcname + ' does not match ' + propertyRe.pattern + ': ' + line
                    enum = m.group(1)
                    prop = m.group(2)
                    properties.setdefault(enum, []).append(prop)

            self.settings.setdefault(metainfFile, []).append(properties)
            return True
        return False

    def __addsrc__(self, arcname, contents):
        return False

    def __closing__(self):
        for metainfFile, propertiesList in self.settings.iteritems():
            arcname = 'META-INF/truffle/' + metainfFile
            lines = []
            counter = 1
            for properties in propertiesList:
                for enum in sorted(properties.viewkeys()):
                    assert enum.startswith(metainfFile)
                    newEnum = metainfFile + str(counter)
                    counter += 1
                    for prop in properties[enum]:
                        lines.append(newEnum + prop)

            content = os.linesep.join(lines)
            self.arc.zf.writestr(arcname, content + os.linesep)

def mx_post_parse_cmd_line(opts):

    def _uses_truffle_dsl_processor(dist):
        for dep in dist.deps:
            if dep.name.startswith('TRUFFLE_DSL_PROCESSOR'):
                return True
        truffle_dsl_processors = set()
        def visit(dep, edge):
            if dep is not dist and dep.isJavaProject():
                for ap in dep.annotation_processors():
                    if ap.name.startswith('TRUFFLE_DSL_PROCESSOR'):
                        truffle_dsl_processors.add(ap)
        dist.walk_deps(visit=visit)
        return len(truffle_dsl_processors) != 0

    for d in mx.dependencies():
        if d.isJARDistribution():
            if _uses_truffle_dsl_processor(d):
                d.set_archiveparticipant(TruffleArchiveParticipant())
