import net.vixns.Utils

def call (team,token,channel,body) {

    try {
        body()    
    }
    catch(err) {
        if (currentBuild.result == null)  {
            currentBuild.result = "FAILURE"
        }
        throw err
    }
    finally {
        switch(currentBuild.result) {
            case "UNSTABLE":
                color = "warning"
                break
            case "FAILURE":
                color = "danger"
                break
            default:
                color = "good"
                currentBuild.result = "SUCCESS"
        }
        slackSend(
            channel: channel, 
            tokenCredentialId: token,
            teamDomain: team,
            color: color, 
            message: "<${env.JOB_URL}|${env.JOB_NAME}> ${env.BRANCH_NAME} build <${env.BUILD_URL}/console|${currentBuild.result}>\n${net.vixns.Utils.summarizeBuild(currentBuild)}"
        )
    }
}