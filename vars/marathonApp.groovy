import net.vixns.Utils

def call (body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    withSlackNotification(config.slack_channel) {
        node {
            checkout scm
            short_commit=gitCommit().take(8)                        
            docker.withRegistry('https://registry.vixns.net/', 'registry_vixns_net') {
                for (def app in config.apps) {
                    
                    if(app.branch != env.BRANCH_NAME) continue

                    if(app.image == null) {
                        def image = "${app.owner}/${app.ns}/${app.group}/${app.name}"
                        app.image = "registry.vixns.net/${image}:${short_commit}"
                        stage("Build docker image ${app.name}") {
                            if(app.basedir == null) {
                                docker.build(image).push(short_commit)
                            } else {
                                docker.build(image,app.basedir).push(short_commit)
                            }
                        }
                    }


                    withCredentials([
                        usernamePassword(credentialsId: "marathonId", usernameVariable: 'MARATHON_USER', passwordVariable: 'MARATHON_PASSWORD'),
                        string(credentialsId: "nasUri-${app.owner}", variable: 'NAS_URI')                        
                        ]) {

                        def filename = "${app.name}-${app.env}.json"
                        def filecontents = readFile("deploy/${app.env}/${app.name}.json")
                                    .replaceAll('_NS_', app.ns)
                                    .replaceAll('_GROUP_', app.group)
                                    .replaceAll('_NAME_', app.name)
                                    .replaceAll('_OWNER_', app.owner)
                                    .replaceAll('_ENV_', app.env)
                                    .replaceAll('_NAS_URI_', env.NAS_URI)
                                    .replaceAll('_DOCKER_IMAGE_', app.image)

                        if (app.mysql_database != null) {
                            withCredentials([
                                usernamePassword(credentialsId: "${app.owner}_${app.ns}_${app.group}_mysql", usernameVariable: 'MYSQL_USER', passwordVariable: 'MYSQL_PASSWORD'),
                                string(credentialsId: "${app.owner}_${app.ns}_mysql_root_password", variable: 'MYSQL_ROOT_PASSWORD')
                                ]) {
                                filecontents = filecontents   
                                    .replaceAll('_MYSQL_USER_', env.MYSQL_USER)
                                    .replaceAll('_MYSQL_PASSWORD_', env.MYSQL_PASSWORD)
                                    .replaceAll('_MYSQL_DATABASE_', app.mysql_database)
                                    .replaceAll('_MYSQL_ROOT_PASSWORD_', env.MYSQL_ROOT_PASSWORD)
                            }
                        } 

                        writeFile(file: filename, text:filecontents)

                        stage("Deploy ${app.ns}/${app.group}/${app.name} to ${app.env}"){
                            //if(app.type == 'chronos') {
                            //    sh("curl -sL -H \"Content-Type: application/json\" -X POST ${chronosUrl}/scheduler/iso8601 -d@${filename}")
                            //} else {
                                marathon(
                                    url: 'https://marathon.vixns.net:8443/',
                                    credentialsId: 'marathonId',
                                    filename: filename,
                                    forceUpdate: true,
                                    appid: "/${app.owner}/${app.ns}/${app.group}/${app.env}/${app.name}",
                                    docker: app.image
                                )
                            //}
                        }
                    }
                }
            }
        }
    }
}