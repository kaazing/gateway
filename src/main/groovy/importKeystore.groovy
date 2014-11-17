def do_importKeystore() {
    def srckeystorefile = project.properties.srckeystore;
    def srcalias = project.properties.srcalias;
    def srcstorepass = project.properties.srcstorepass;
    def destkeystore = project.properties.destkeystore;
    def deststorepass = project.properties.deststorepass;
    def destalias = project.properties.destalias;
    def deststoretype = project.properties.deststoretype;
    def destkeypass = project.properties.destkeypass;

    def cmd = "keytool -importkeystore -srckeystore "+srckeystorefile+
            " -srcalias " + srcalias +
            " -srcstorepass " + srcstorepass +
            " -destkeystore " + destkeystore +
            " -deststorepass " + deststorepass +
            " -destkeypass " + destkeypass +
            " -destalias " + destalias +
            " -deststoretype " + deststoretype;

    log.info("Executing: " + cmd);


    def process = cmd.execute();

    process
    process.waitFor()
    process.in.eachLine { line ->
        log.info "> ${line}";
    }
}

do_importKeystore()
