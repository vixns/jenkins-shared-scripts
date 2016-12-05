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
}