package com.mycompany.app;
import java.io.BufferedReader;
import java.util.List;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.IOException;

public class ModifiedWalkForwardReader {

    private BufferedReader  br;
    private List<Integer>   counterResults;     // Results of a single walk [ numelementsTraining, numbugsTraining, numelementsTest, numbugsTest ].
    private int             step;               // Step size.
    private int             steps;              // Number of steps to divide the dataset into.
    private int             size;               // Number of lines of the file specified in path.
    private String          path;               // filepath.


    public ModifiedWalkForwardReader( int steps, String path ) throws IOException {
        this.steps = steps;
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
        return this.step;
    }
    public int getSteps(){
        return this.steps;
    }

    
    public void getDatasetSize() throws IOException {
        int lines = 0;
        for( String l = this.br.readLine(); l != null; l = this.br.readLine() ){
            lines ++;
        }
        if( lines > 2 ){
            this.size = lines -1;
        }
        this.step = (int) Math.floor( (double) this.size / this.steps );
        this.br = new BufferedReader( new FileReader( path ) );
    }

    public void appendCounterResult( int value ){
        this.counterResults.add( value );
    }

    public void reset() throws IOException {
        this.br = new BufferedReader( new FileReader( path ) );
        this.counterResults.clear();
    }

}
