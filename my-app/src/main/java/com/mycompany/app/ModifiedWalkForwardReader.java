import java.io.BufferedReader;

public class ModifiedWalkForwardReader {

    private BufferedReader  br;
    private List<Integer>   counterResults;     // Results of a single walk [ numelements, numbugs ].
    private int             STEP;               // Step size.
    private int             STEPS;              // Number of steps to divide the dataset into.
    private int             SIZE;               // Number of lines of the file specified in path.
    private String          path;               // filepath.


    public ModifiedWalkForwardReader( int STEPS, String path ){
        this.STEPS = STEPS;
        this.path = path;
        this.br = new BufferedReader( new FileReader( path ) );
        this.counterResults = new ArrayList<>();
        this.getDatasetSize();
    }


    public void setBr( BufferedReader br ){
        this.br = br;
    }
    public void setCounterResults( List<Integer>   counterResults ){
        this.counterResults = counterResults;
    }
    public BufferedReader getBr( ){
        return this.br;
    }
    public List<Integer> getCounterResults( ){
        return this.counterResults;
    }
    public int getStep(){
        return this.STEP;
    }
    public int getSteps(){
        return this.STEPS;
    }

    
    public void getDatasetSize(){
        int lines = 0;
        while( this.br.readLine() != null ){
            lines ++;
        }
        if( lines > 2 ){
            this.SIZE = lines -1;
        }
        this.STEP = (int) this.SIZE/this.STEPS;
        this.br = new BufferedReader( new FileReader( path ) );
    }
}
