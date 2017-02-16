#!groovy

def call (template, app) {

    if (app.owner == null) app.owner = env.OWNER
    if (app.owner == null) error('cannot find owner as app.owner or env.OWNER')

    if (app.ns == null) app.ns = app.env

    def filecontents = template
                .replaceAll('_NS_', app.ns)
                .replaceAll('_GROUP_', app.group)
                .replaceAll('_OWNER_', app.owner)
                .replaceAll('_NAME_', app.name)
                .replaceAll('_ENV_', app.env)
                .replaceAll('_NAS_URI_', env.NAS_URI)
                .replaceAll('_DOCKER_IMAGE_', app.image)
                .replaceAll('_VHOST_', app.vhost)
                .replaceAll('_DRUPAL_SALT_', env.DRUPAL_SALT)


    if (app.command != null) {
    	def command = app.command.regular
        
        filecontents = filecontents   
            .replaceAll('_COMMAND_', command)
            .replaceAll('_PERIOD_MINUTES_', app.command.period_minutes)
    } 

    if (app.mysql != null) {
        if(app.mysql.port == null) app.mysql.port = "3306"

        def mysqlCredentialsPrefix = "${app.owner}_${app.ns}"
        if(app.group != null) mysqlCredentialsPrefix = "${app.owner}_${app.ns}_${app.group}"

        withCredentials([
            usernamePassword(credentialsId: "${mysqlCredentialsPrefix}_mysql", usernameVariable: 'MYSQL_USER', passwordVariable: 'MYSQL_PASSWORD'),
            string(credentialsId: "${app.owner}_${app.ns}_mysql_root_password", variable: 'MYSQL_ROOT_PASSWORD')
            ]) {
            filecontents = filecontents   
                .replaceAll('_MYSQL_USER_', env.MYSQL_USER)
                .replaceAll('_MYSQL_PASSWORD_', env.MYSQL_PASSWORD)
                .replaceAll('_MYSQL_HOST_', app.mysql.host)
                .replaceAll('_MYSQL_PORT_', app.mysql.port)
                .replaceAll('_MYSQL_DATABASE_', app.mysql.database)
                .replaceAll('_MYSQL_ROOT_PASSWORD_', env.MYSQL_ROOT_PASSWORD)
        }
    } 

    if (app.drupal != null) {
        if(app.drupal.salt == null) {
            try {
                withCredentials([string(credentialsId: 'drupalSalt', variable: 'DRUPAL_SALT')]) {
                    filecontents = filecontents   
                        .replaceAll('_DRUPAL_SALT_', env.DRUPAL_SALT)
                }
            } catch (_) {
                error("cannot find drupal salt, add drupal.salt key to your app or set a global salt in the drupalSalt secret text credential.")
            }
        } else {
            filecontents = filecontents   
                .replaceAll('_DRUPAL_SALT_', app.drupal.salt)
        }    
    }

    if (app.smtp != null) {
        if(app.smtp.port == null) app.smtp.port = "25"
        filecontents = filecontents   
            .replaceAll('_SMTP_HOST_', app.smtp.host)
            .replaceAll('_SMTP_PORT_', app.smtp.port)
    } 

    if (app.sentry != null) {
        if(app.sentry.dsn == null) {
            withCredentials([
                string(credentialsId: "${app.env}_sentry_dsn", variable: 'SENTRY_DSN'),                     
                string(credentialsId: "${app.env}_sentry_public_dsn", variable: 'SENTRY_PUBLIC_DSN')                   
                ]) {
                filecontents = filecontents   
                    .replaceAll('_SENTRY_DSN_', env.SENTRY_DSN)
                    .replaceAll('_SENTRY_PUBLIC_DSN_', env.SENTRY_PUBLIC_DSN)            
            }
        } else {
                filecontents = filecontents   
                    .replaceAll('_SENTRY_DSN_', app.sentry.dsn)
                    .replaceAll('_SENTRY_PUBLIC_DSN_', app.sentry.public_dsn)                        
        }
    }
    if (app.solr != null) {
        if(app.solr.port == null) app.solr.port = "8389"
        filecontents = filecontents   
            .replaceAll('_SOLR_HOST_', app.solr.host)
            .replaceAll('_SOLR_PORT_', app.solr.port)
    } 

    if (app.memcache != null) {
        if(app.memcache.port == null) app.memcache.port = "11211"
        filecontents = filecontents   
            .replaceAll('_MEMCACHE_HOST_', app.memcache.host)
            .replaceAll('_MEMCACHE_PORT_', app.memcache.port)
    } 

    if (app.varnish != null) {
        if(app.varnish.port == null) app.varnish.port = "6082"
        withCredentials([
            string(credentialsId: "ifprofs_varnish_key", variable: 'VARNISH_KEY'),
            string(credentialsId: "ifprofs_varnish_passphrase", variable: 'VARNISH_PASSPHRASE')
            ]) {
            filecontents = filecontents   
                .replaceAll('_VARNISH_HOST_', app.varnish.host)
                .replaceAll('_VARNISH_PORT_', app.varnish.port)
                .replaceAll('_VARNISH_KEY_', env.VARNISH_KEY)
                .replaceAll('_VARNISH_PASSPHRASE_', env.VARNISH_PASSPHRASE)
        }
    }
    filecontents
}
