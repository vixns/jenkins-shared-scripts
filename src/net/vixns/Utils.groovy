package net.vixns

class Utils {
    
    @NonCPS
    static def summarizeBuild(b) {
        b.changeSets.collect { cs ->
            "Changes\n" + cs.collect { entry ->
                /- ${entry.msg} [${entry.author.fullName}]/
            }.join('\n')
        }.join('\n')
    }

    static boolean hasBreakingChangeTag(def script) {
    	def name = gitTagName(script)
    	if (name == null) return false
        def match = name =~ /\+bc$/
        def result = match
        match = null // prevent serialisation
        return result
    }

    static String gitTagName(def script) {
        def commit = getCommit(script)
        if (commit) {
            def desc = script.sh(script: "git describe --always --tags ${commit}", returnStdout: true)?.trim()
            if (desc =~ /.+-[0-9]+-g[0-9A-Fa-f]{6,}$/ || desc == commit.take(7)) {
                return null
            }
            return desc
        }
        return null
    }

    static String getCommit(def script) {
        return script.sh(script: 'git rev-parse HEAD', returnStdout: true)?.trim()
    }
}