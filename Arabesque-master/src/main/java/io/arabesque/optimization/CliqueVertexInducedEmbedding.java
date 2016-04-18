package io.arabesque.optimization;

import org.apache.log4j.Logger;

import io.arabesque.computation.BasicComputation;
import io.arabesque.embedding.VertexInducedEmbedding;
import io.arabesque.utils.collection.IntArrayList;
import net.openhft.koloboke.collect.IntCollection;

public class CliqueVertexInducedEmbedding extends VertexInducedEmbedding {
    private static final Logger LOG = Logger.getLogger(BasicComputation.class);

    @Override
    public IntCollection getExtensibleWordIds() {
        if (dirtyExtensionWordIds) {
            extensionWordIds.clear();

            IntCollection lastVertexNeighbours = mainGraph.getVertexNeighbours(getVertices().getLast());
        	LOG.info("lastVertexNeighbours....................: " + lastVertexNeighbours.toString());

            if (lastVertexNeighbours != null) {
                intAddConsumer.setCollection(extensionWordIds);
                lastVertexNeighbours.forEach(intAddConsumer);
            }

            int numVertices = getNumVertices();
            IntArrayList vertices = getVertices();
        	LOG.info("extensionWordIds....................: " + extensionWordIds);

            // Clean the words that are already in the embedding
            for (int i = 0; i < numVertices; ++i) {
                int wId = vertices.getUnchecked(i);
                extensionWordIds.removeInt(wId);
            }
        }
    	LOG.info("extensionWordIds....................: " + extensionWordIds);

        return extensionWordIds;
    }

    @Override
    public boolean isCanonicalEmbeddingWithWord(int wordId) {
        if (this.getNumVertices() == 0) return true;

        return wordId > getVertices().getLast();
    }
}
