import net.vixns.Utils

def call (body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def slaskTeam = (config.slack != null && config.slack.team != null) ? config.slack.team : 'vixns'
    def slaskToken = (config.slack != null && config.slack.token != null) ? config.slack.token : 'vixns_token_id'
    def slaskChannel = (config.slack != null && config.slack.channel != null) ? config.slack.channel : config.slack_channel

    withSlackNotification(slaskTeam,slaskToken,slaskChannel) {
        node {
            checkout scm
            def git_tag = net.vixns.Utils.gitTagName(this)
            def docker_label = git_tag
            if (docker_label == null) docker_label = net.vixns.Utils.getCommit(this).take(8)

            docker.withRegistry('https://registry.vixns.net/', 'registry_vixns_net') {
                for (def app in config.apps) {
                    
                    if(app.branch != env.BRANCH_NAME) continue
                    if(app.tagged_only && git_tag == null) continue

                    if(app.image == null) {
                        def image = "${app.owner}/${app.ns}/${app.group}/${app.name}"
                        app.image = "registry.vixns.net/${image}:${docker_label}"
                        stage("Build docker image ${app.name}") {
                            if(app.basedir == null) {
                                docker.build(image).push(docker_label)
                            } else {
                                docker.build(image,app.basedir).push(docker_label)
                            }
                        }
                    }

                    if(app.framework == null) app.framework = 'marathon';

                    withCredentials([
                        usernamePassword(credentialsId: "marathonId", usernameVariable: 'MARATHON_USER', passwordVariable: 'MARATHON_PASSWORD'),
                        string(credentialsId: "nasUri-${app.owner}", variable: 'NAS_URI')                        
                        ]) {

                        def filename = "${app.name}-${app.env}.json"
                        writeFile(
                            file: filename,
                            text: reifyTemplate(readFile("deploy/${app.env}/${app.name}.json"), app)
                        )
                        if(app.framework == 'marathon') {
                            stage("Deploy ${app.ns}/${app.group}/${app.name} to ${app.env}"){
                                marathon(
                                    url: 'https://marathon.vixns.net:8443/',
                                    credentialsId: 'marathonId',
                                    filename: filename,
                                    forceUpdate: true,
                                    appid: "/${app.owner}/${app.ns}/${app.group}/${app.env}/${app.name}",
                                    docker: app.image
                                )
                            }
                        } else {
                            error("can only deploy for marathon framework.")
                        }
                    }
                }
            }
        }
    }
}