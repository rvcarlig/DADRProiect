
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class sss {

	final static int threadCount = 25;
	final static int intStart = 10000000;
	final static int intEnd = 99999999;
	
	public static void main(String[] args) {
		String H ="";
		if(args.length>=1)
			H=args[0];
		/*sss pd = new sss();
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		
		Collection<PasswordTester> collection = new ArrayList<PasswordTester>( );
		
		int subIntNr = (intEnd- intStart)/threadCount;
		for(int i=0; i< threadCount; i++){
			PasswordTester task = pd.new PasswordTester("Johnny"+i+"_",i*subIntNr,(i+1)*subIntNr-1,H);
		    collection.add(task);
		  }
		
		try {
			//this is called when one of the threads returns something other than null
			// all other threads are canceled
			String f=executor.invokeAny(collection);
			System.out.println(f);
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		executor.shutdown();
		*/
		System.out.println(H);
	}
   
   class PasswordTester implements Callable<String> {

		String myName="";
		int intStart;
		int intEnd;
		String hash;
		
		/**
		 * @param intervalStart int
		 * @param intervalEnd int
		 * @param hash String
		 * @param name name of the PasswordTester instance
		 */
		public PasswordTester(String name, int start, int end, String H){
			myName=name;//save name locally in this class
			intStart = start;
			intEnd = end;
			hash = H;		
			// at this point , name dies!!
		}
		
		
		
		@Override
		public String call() throws Exception {
			
			for(int i=intStart;i<intEnd;i++){
				String tempHash = sha256(i);
				if (hash.equals(tempHash)){
					return String.valueOf(i);
				}
			}		
			return null; 
		}
		
		
		String sha256(int possiblePassword) {
			 try{
			        MessageDigest digest = MessageDigest.getInstance("SHA-256");
			        byte[] hash = digest.digest((""+possiblePassword).getBytes("UTF-8"));
			        StringBuffer hexString = new StringBuffer();

			        for (int i = 0; i < hash.length; i++) {
			            String hex = Integer.toHexString(0xff & hash[i]);
			            if(hex.length() == 1) hexString.append('0');
			            hexString.append(hex);
			        }

			        return hexString.toString();
			    } catch(Exception ex){
			       throw new RuntimeException(ex);
			    }
		}

	}
}