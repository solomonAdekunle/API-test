package helpers;

public class Env {

    // Default : signifies running service locally
    public static final String LOCAL = "local";

    // Signifies running the service locally within docker
    private static final String LOCAL_DOCKER = "local-docker";

    public static String get() {
        String env =  System.getProperty("env");

        // if set to "", default to local
        return "".equals(env) ? LOCAL : env;
    }

    public static boolean isLocalDocker() {
        return "true".equals(System.getProperty(LOCAL_DOCKER));
    }

    public static boolean isEnvStatic1OrProd(){
        return get().equals("static1") || get().equals("prod");
    }
}
