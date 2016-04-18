package io.arabesque.examples.clique;

import org.apache.log4j.Logger;

import io.arabesque.computation.BasicComputation;
import io.arabesque.computation.VertexInducedComputation;
import io.arabesque.conf.Configuration;
import io.arabesque.embedding.VertexInducedEmbedding;

public class CliqueComputation extends VertexInducedComputation<VertexInducedEmbedding> {
    private static final String MAXSIZE = "arabesque.clique.maxsize";
    private static final int MAXSIZE_DEFAULT = 4;
    private static final Logger LOG = Logger.getLogger(BasicComputation.class);

    int maxsize;

    @Override
    public void init() {
        super.init();
    	LOG.info("Clique computation....................: " );

        maxsize = Configuration.get().getInteger(MAXSIZE, MAXSIZE_DEFAULT);
    }

    @Override
    public boolean filter(VertexInducedEmbedding embedding) {
        return isClique(embedding);
    }

    public boolean isClique(VertexInducedEmbedding embedding) {
        return embedding.getNumEdgesAddedWithExpansion() == embedding.getNumVertices() - 1;
    }

    @Override
    public boolean shouldExpand(VertexInducedEmbedding embedding) {
        return embedding.getNumVertices() < maxsize;
    }

    @Override
    public void process(VertexInducedEmbedding embedding) {
        if (embedding.getNumVertices() == maxsize) {
            output(embedding);
        }
    }
}
