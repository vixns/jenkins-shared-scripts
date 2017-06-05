#!groovy

def call (template, app) {

    if (app.owner == null) app.owner = env.OWNER
    if (app.owner == null) error('cannot find owner as app.owner or env.OWNER')

    if (app.ns == null) app.ns = app.env

    def filecontents = template
                .replaceAll('_NS_', app.ns)
                .replaceAll('_GROUP_', (app.group == null) ? '' : app.group)
                .replaceAll('_OWNER_', app.owner)
                .replaceAll('_NAME_', app.name)
                .replaceAll('_ENV_', app.env)
                .replaceAll('_NAS_URI_', env.NAS_URI)
                .replaceAll('_DOCKER_IMAGE_', app.image)

    if (app.vhost != null)
        filecontents = filecontents
            .replaceAll('_VHOST_', app.vhost)

    if (app.hc_path != null)
        filecontents = filecontents
            .replaceAll('_HC_PATH_', app.hc_path)

    if (app.command != null) {
        def command = (app.hasBreakingChanges != null && app.hasBreakingChanges) ? app.command.bc : app.command.regular
        filecontents = filecontents   
            .replaceAll('_COMMAND_', command)
            .replaceAll('_PERIOD_MINUTES_', (app.command.period_minutes != null) ? app.command.period_minutes : "15")
    } 

    if (app.sentry != null) {
        withCredentials([
            string(credentialsId: "sentry_dsn", variable: 'SENTRY_DSN'),
            string(credentialsId: "sentry_public_dsn", variable: 'SENTRY_PUBLIC_DSN'),
            ]) {
            filecontents = filecontents
                .replaceAll('_SENTRY_DSN_', env.SENTRY_DSN)
                .replaceAll('_SENTRY_PUBLIC_DSN_', env.SENTRY_PUBLIC_DSN)
        }
    }

    if (app.http_auth != null) {
        withCredentials([
            usernamePassword(credentialsId: "http_auth", usernameVariable: 'HTTP_USER', passwordVariable: 'HTTP_PASS'),
            string(credentialsId: "sentry_public_dsn", variable: 'SENTRY_PUBLIC_DSN'),
            ]) {
            filecontents = filecontents
                .replaceAll('_HTTP_USER_', env.HTTP_USER)
                .replaceAll('_HTTP_PASS_', env.HTTP_PASS)
        }
    }
    if (app.mysql != null) {
        if(app.mysql.port == null) app.mysql.port = "3306"

        def mysqlCredentialsPrefix = "${app.owner}_${app.ns}"
        if(app.group != null) mysqlCredentialsPrefix = "${app.owner}_${app.ns}_${app.group}"

        withCredentials([
            usernamePassword(credentialsId: "${mysqlCredentialsPrefix}_mysql", usernameVariable: 'MYSQL_USER', passwordVariable: 'MYSQL_PASSWORD')
            ]) {
            filecontents = filecontents   
                .replaceAll('_MYSQL_USER_', env.MYSQL_USER)
                .replaceAll('_MYSQL_PASSWORD_', env.MYSQL_PASSWORD)
                .replaceAll('_MYSQL_HOST_', app.mysql.host)
                .replaceAll('_MYSQL_PORT_', app.mysql.port)
                .replaceAll('_MYSQL_DATABASE_', app.mysql.database)
        }
    } 

    if (app.drupal != null) {
        if(app.drupal instanceof Boolean || app.drupal.salt == null) {
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
        if(app.sentry instanceof Boolean || app.sentry.dsn == null) {
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
        if(app.memcache.prefix == null) app.memcache.prefix = ""
        filecontents = filecontents   
            .replaceAll('_MEMCACHE_HOST_', app.memcache.host)
            .replaceAll('_MEMCACHE_PORT_', app.memcache.port)
            .replaceAll('_MEMCACHE_PREFIX_', app.memcache.prefix)
    } 

    if (app.varnish != null) {
        if(app.varnish.port == null) app.varnish.port = "6082"
        filecontents = filecontents   
            .replaceAll('_VARNISH_HOST_', app.varnish.host)
            .replaceAll('_VARNISH_PORT_', app.varnish.port)

        if(app.varnish.key == null) {
            withCredentials([
                string(credentialsId: "varnish_key", variable: 'VARNISH_KEY'),
                string(credentialsId: "varnish_passphrase", variable: 'VARNISH_PASSPHRASE')
                ]) {
                filecontents = filecontents   
                    .replaceAll('_VARNISH_KEY_', env.VARNISH_KEY)
                    .replaceAll('_VARNISH_PASSPHRASE_', env.VARNISH_PASSPHRASE)
            }
        } else {
                filecontents = filecontents   
                    .replaceAll('_VARNISH_KEY_', app.varnish.key)
                    .replaceAll('_VARNISH_PASSPHRASE_', app.varnish.passphrase)            
        }
    }

    filecontents
}
