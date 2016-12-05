#!groovy

def call() {
  if (env.GIT_COMMIT == null) {
    env.GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
  }
  env.GIT_COMMIT
}