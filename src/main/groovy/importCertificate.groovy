def do_importCertificate() {
    def crtfile = project.properties.file;
    def keystorefile = project.properties.keystore;
    def storepass = project.properties.storepass;
    def alias = project.properties.alias;
    def trustcacerts = Boolean.valueOf(project.properties.trustcacerts);
  
    def echoCmd = "echo yes"
    def cmd = "keytool -importcert -keystore "+keystorefile+
              " -file "+crtfile+
              " -storepass "+storepass+
              " -alias "+alias
              trustcacerts? " -trustcacerts" : "";

    log.info("Executing: "+echoCmd+ " | "+cmd);


    def echoProcess = echoCmd.execute();
    def process = cmd.execute();
  
    echoProcess | process
    process.waitFor()
    process.in.eachLine { line ->
	log.info "> ${line}";
    }
}

do_importCertificate()
