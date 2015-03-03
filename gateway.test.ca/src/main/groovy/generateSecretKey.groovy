def do_importKeystore() {
    def alias = project.properties.alias;
    def keyalg = project.properties.keyalg;
    def keysize = project.properties.keysize;
    def storetype = project.properties.storetype;
    def keystore = project.properties.keystore;
    def storepass = project.properties.storepass;
    def keypass = project.properties.keypass;

    def cmd = "keytool -genseckey -alias " + alias +
            " -keyalg " + keyalg +
            " -keysize " + keysize +
            " -storetype " + storetype +
            " -keystore " + keystore +
            " -storepass " + storepass +
            " -keypass " + keypass;

    log.info("Executing: " + cmd);


    def process = cmd.execute();

    process
    process.waitFor()
    process.in.eachLine { line ->
        log.info "> ${line}";
    }
}

do_importKeystore()
