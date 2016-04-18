package io.arabesque.computation;

import io.arabesque.aggregation.AggregationStorage;
import io.arabesque.conf.Configuration;
import io.arabesque.embedding.Embedding;
import io.arabesque.embedding.VertexInducedEmbedding;
import io.arabesque.graph.MainGraph;
import io.arabesque.pattern.Pattern;
import net.openhft.koloboke.collect.IntCollection;
import net.openhft.koloboke.collect.set.hash.HashIntSet;
import net.openhft.koloboke.collect.set.hash.HashIntSets;
import net.openhft.koloboke.function.IntConsumer;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.apache.log4j.Logger;

public abstract class BasicComputation<E extends Embedding> implements Computation<E> {
    private static final Logger LOG = Logger.getLogger(BasicComputation.class);

    private boolean outputEnabled;

    private ExecutionEngine<E> underlyingExecutionEngine;
    private MainGraph mainGraph;
    private Configuration configuration;
    private IntConsumer expandConsumer;
    private long numChildrenEvaluated = 0;
    private long numExpendChildrenSetSize = 0;  //yuyu add
    private E currentEmbedding;

    @Override
    public final void setUnderlyingExecutionEngine(ExecutionEngine<E> underlyingExecutionEngine) {
        this.underlyingExecutionEngine = underlyingExecutionEngine;
    }

    public MainGraph getMainGraph() {
        return mainGraph;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init() {
        expandConsumer = new IntConsumer() {
            @Override
            public void accept(int wordId) {
//            	LOG.info("wordId....................: " + wordId);
                doExpandFilter(wordId);
            }
        };

        mainGraph = Configuration.get().getMainGraph();
        configuration = Configuration.get();
        numChildrenEvaluated = 0;
        outputEnabled = Configuration.get().isOutputActive();
    }

    @Override
    public void initAggregations() {
        // Empty by default
    }

    @Override
    public void expand(E embedding) {
        if (getStep() > 0) {
            if (!aggregationFilter(embedding)) {  // true
                return;
            }

            aggregationProcess(embedding);    // Empty by default
        }
        LOG.info("embedding...................: " + embedding.toOutputString());
        LOG.info("embedding Vertices()...................: " + embedding.getVertices().toString());
        LOG.info("embedding..Edges().................: " + embedding.getEdges().toString());
        IntCollection possibleExtensions = getPossibleExtensions(embedding);
    	LOG.info("getPossibleExtensions(embedding)...................: " + getPossibleExtensions(embedding).toString());

        if (possibleExtensions != null) {
            filter(embedding, possibleExtensions);   // Do nothing by default
        }

        if (possibleExtensions == null || possibleExtensions.isEmpty()) {
            handleNoExpansions(embedding);
            return;
        }
        currentEmbedding = embedding;
        possibleExtensions.forEach(expandConsumer);
    }

    private void doExpandFilter(int wordId) {
    	numExpendChildrenSetSize ++;//yuyu add
    	LOG.info("doExpandFilter...................: ");
    	
        if (filter(currentEmbedding, wordId)) { // return existingEmbedding.isCanonicalEmbeddingWithWord(newWord);
            currentEmbedding.addWord(wordId);

            if (filter(currentEmbedding)) { //return isClique(embedding);
                if (shouldExpand(currentEmbedding)) {  //return embedding.getNumVertices() < maxsize;
                    underlyingExecutionEngine.processExpansion(currentEmbedding);  
                }

                numChildrenEvaluated++;
                process(currentEmbedding); //if (embedding.getNumVertices() == maxsize) {output(embedding);
                
            }
            currentEmbedding.removeLastWord();
        }

    }

    @Override
    public void handleNoExpansions(E embedding) {
        // Empty by default
    }

    private IntCollection getPossibleExtensions(E embedding) {
        if (embedding.getNumWords() > 0) { 
            return embedding.getExtensibleWordIds(); //public class CliqueVertexInducedEmbedding extends VertexInducedEmbedding {
        } else {
            return getInitialExtensions();
        }
    }

    protected HashIntSet getInitialExtensions() {
        int totalNumWords = getInitialNumWords();
        int numPartitions = getNumberPartitions();
        int myPartitionId = getPartitionId();
        int numWordsPerPartition = Math.max(totalNumWords / numPartitions, 1);
        int startMyWordRange = myPartitionId * numWordsPerPartition;
        int endMyWordRange = startMyWordRange + numWordsPerPartition;

        // If we are the last partition or our range end goes over the total number
        // of vertices, set the range end to the total number of vertices.
        if (myPartitionId == numPartitions - 1 || endMyWordRange > totalNumWords) {
            endMyWordRange = totalNumWords;
        }

        // TODO: Replace this by a list implementing IntCollection. No need for set.
        HashIntSet initialExtensions = HashIntSets.newMutableSet(numWordsPerPartition);

        for (int i = startMyWordRange; i < endMyWordRange; ++i) {
            initialExtensions.add(i);
        }

        return initialExtensions;
    }

    protected abstract int getInitialNumWords();

    @Override
    public boolean shouldExpand(E embedding) {
        return true;
    }

    @Override
    public void filter(E existingEmbedding, IntCollection extensionPoints) {
        // Do nothing by default
    }

    @Override
    public boolean filter(E existingEmbedding, int newWord) {
        return existingEmbedding.isCanonicalEmbeddingWithWord(newWord);
    }

    @Override
    public <K extends Writable, V extends Writable> AggregationStorage<K, V> readAggregation(String name) {
        return underlyingExecutionEngine.getAggregatedValue(name);
    }

    @Override
    public <K extends Writable, V extends Writable> void map(String name, K key, V value) {
        underlyingExecutionEngine.map(name, key, value);
    }

    @Override
    public int getPartitionId() {
        return underlyingExecutionEngine.getPartitionId();
    }

    @Override
    public int getNumberPartitions() {
        WorkerContext workerContext = underlyingExecutionEngine.getWorkerContext();
        return workerContext.getNumberPartitions();
    }

    @Override
    public final int getStep() {
        // When we achieve steps that reach long values, the universe
        // will probably have ended anyway
        return (int) underlyingExecutionEngine.getSuperstep();
    }

    @Override
    public boolean filter(E newEmbedding) {
        return true;
    }

    @Override
    public boolean aggregationFilter(E Embedding) {
        return true;
    }

    @Override
    public boolean aggregationFilter(Pattern pattern) {
        return true;
    }

    @Override
    public void aggregationProcess(E embedding) {
        // Empty by default
    }

    @Override
    public void finish() {
        LongWritable longWritable = new LongWritable();

        LOG.info("Num children evaluated: " + numChildrenEvaluated);
        longWritable.set(numChildrenEvaluated);
        LOG.info("Num numExpendChildrenSetSize: " + numExpendChildrenSetSize);
        longWritable.set(numExpendChildrenSetSize);
        underlyingExecutionEngine.aggregate(MasterExecutionEngine.AGG_CHILDREN_EVALUATED, longWritable);
    }

    public void output(Embedding embedding) {
        if (outputEnabled) {
            output(embedding.toOutputString());
        }
    }

    @Override
    public void output(String outputString) {
        underlyingExecutionEngine.output(outputString);
    }
}
