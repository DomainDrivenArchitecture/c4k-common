from os import environ
from subprocess import run
from pybuilder.core import init, task
from ddadevops import *

default_task = "dev"

name = "c4k-common"
MODULE = "not-used"
PROJECT_ROOT_PATH = "."


@init
def initialize(project):
    input = {
        "name": name,
        "module": MODULE,
        "stage": "notused",
        "project_root_path": PROJECT_ROOT_PATH,
        "build_types": [],
        "mixin_types": ["RELEASE"],
        "release_primary_build_file": "project.clj",
        "release_secondary_build_files": [
            "project-cljs.clj",
        ],
        "release_main_branch": "main",
    }

    build = ReleaseMixin(project, input)
    build.initialize_build_dir()


@task
def test_clj(project):
    run("lein test", shell=True, check=True)


@task
def test_cljs(project):
    run("shadow-cljs compile test", shell=True, check=True)
    run("node target/node-tests.js", shell=True, check=True)


@task
def upload_clj(project):
    run("lein deploy", shell=True, check=True)


@task
def upload_cljs(project):
    run(
        "mv project.clj project-clj.clj && mv project-cljs.clj project.clj",
        shell=True,
        check=True,
    )
    run("lein deploy", shell=True, check=True)
    run(
        "mv project.clj project-cljs.clj && mv project-clj.clj project.clj",
        shell=True,
        check=True,
    )


@task
def lint(project):
    # TODO: Do proper configuration
    """run(
        "lein eastwood",
        shell=True,
        check=True,
    )"""
    run(
        "lein ancient check",
        shell=True,
        check=True,
    )


@task
def patch(project):
    linttest(project, "PATCH")
    release(project)


@task
def minor(project):
    linttest(project, "MINOR")
    release(project)


@task
def major(project):
    linttest(project, "MAJOR")
    release(project)


@task
def dev(project):
    linttest(project, "NONE")


@task
def prepare(project):
    build = get_devops_build(project)
    build.prepare_release()


@task
def tag(project):
    build = get_devops_build(project)
    build.tag_bump_and_push_release()


def release(project):
    prepare(project)
    tag(project)


def linttest(project, release_type):
    build = get_devops_build(project)
    build.update_release_type(release_type)
    test_clj(project)
    test_cljs(project)
    lint(project)
