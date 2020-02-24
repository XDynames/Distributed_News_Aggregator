import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

// Updates held client content backups
public class Checkpointer extends Worker implements Runnable{
		public Checkpointer(Job job, AggregationServer server)
			{ super(job, server); }
			
		// Writes the client's meta information to <uuid>.meta
		private void writeMetaFile(String dir) throws IOException{
			String root = dir + mJob.getUUID();
			Path metaPath = Paths.get(root + ".meta");
			writeFile(getWrittableMeta(), metaPath, root);
		}

		// Writes the client's XML content to <uuid>.xml
		private void writeContentFile(String dir) throws IOException{
			String root = dir + mJob.getUUID();
			Path contentPath = Paths.get(root + ".xml");
			writeFile(mJob.getContent(), contentPath, root);
		}

		// Writes the content to disk as a .tmp file before overwriting
		// the destination file 
		private void writeFile(String toWrite, Path writePath, String root)
														throws IOException {
			Path tmpPath = Paths.get(root + ".tmp");
			// Write file to tmp
			Files.write(tmpPath, toWrite.getBytes());
			// Overwrite previous content file
			Files.move(tmpPath, writePath, REPLACE_EXISTING);	
		}

		// Writes client content to file if present
		private void maybeUpdateContentFile(String root) 
									throws InterruptedException, IOException{
			if (mJob.getContent() != null)
				{ writeContentFile(root); }
		}

		// Creates a folder for storing backups if required
		private void maybeCreateFolder(String dir) throws IOException{
			if (!Files.exists(Paths.get(dir)))
    			{ Files.createDirectories(Paths.get(dir)); }
		}

		// Assembles clients time information into a string
		private String getWrittableMeta(){
			String meta = "TimeStamp," + mJob.getTimeStamp().toString();
			meta += "\r\n" + "LogicalTime," + mClock.getTime() +"\r\n";
			return meta;
		}

		public void run(){
			// Root directory for storing backups
			String dir = "./Server/Checkpoint/";
			try{
				mFileLock.acquire();

				// Critical Section
				maybeCreateFolder(dir);
    			writeMetaFile(dir);
				maybeUpdateContentFile(dir);				
				// Critical Section
				
				mFileLock.release();
			}catch (Exception e){ e.printStackTrace(); }
			if (TEST && mLogger != null){
				try{ writeToLog("Checkpoint Update", mJob.getUUID()); }
				catch (Exception e){ e.printStackTrace(); }
			}
		}
}