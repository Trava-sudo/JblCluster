package hub.ebb.jblcluster.eventservice.generator;


public class Runner {
    public static void main(String[] args)  {

        if (args.length == 0) {
            System.out.printf("ERROR: Missing Event microservice Root Path parameter");
            System.exit(4);
        }

        String jblRootPath = args[0];
        System.out.println("Calling Events Factory Generator with param: " + jblRootPath);
        try {
            JblFactoryGenerator generator = JblFactoryGenerator.getInstance();
            generator.generateFactory(jblRootPath);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error -> " + e.getMessage());
        }
    }
}
