public class LamportClock{
	
	private int mTime;

	public LamportClock(){ mTime = 1; }
	public LamportClock(int clock){ mTime = clock; }

	// Returns the current logical time on the clock
	public int getTime(){ return mTime; }
	
	// Increments the logical time on the clock
	public synchronized void incrementTime(){ mTime++; }
	/* 
		Compares the incoming time with the current time,
	 	setting the clock to the maximum of the two before
		incrementing the clock
		Args: - incomingTime: logical time of incoming message
	*/
	public synchronized void incrementTime(int incomingTime){
		if (incomingTime > mTime){ mTime = incomingTime; }
		mTime++;
	}
	/* 
		Uses an input logical time to set the clock without
		incrementing the time. Will not adjust the clock
		to a time before the current time
	 	Args: - incomingTime: logical time to be loaded
	*/
	public void loadTime(int incomingTime)
		{ if (incomingTime > mTime) { mTime = incomingTime;} }
}