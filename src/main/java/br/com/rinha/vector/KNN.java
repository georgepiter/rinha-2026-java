package br.com.rinha.vector;

import br.com.rinha.model.Models.Reference;
import java.util.PriorityQueue;
import java.util.List;

public class KNN {
    public static class Neighbor implements Comparable<Neighbor> {
        double distSq;
        public boolean isFraud;

        Neighbor(double distSq, boolean isFraud) {
            this.distSq = distSq;
            this.isFraud = isFraud;
        }

        @Override
        public int compareTo(Neighbor o) {
            return Double.compare(o.distSq, this.distSq); // Max-heap
        }
    }

    public static double findKNearest(double[] target, List<Reference> references, int k) {
        PriorityQueue<Neighbor> pq = new PriorityQueue<>(k);

        for (Reference ref : references) {
            double distSq = Vectorizer.euclideanDistanceSq(target, ref.vector);
            if (pq.size() < k) {
                pq.add(new Neighbor(distSq, "fraud".equals(ref.label)));
            } else if (distSq < pq.peek().distSq) {
                pq.poll();
                pq.add(new Neighbor(distSq, "fraud".equals(ref.label)));
            }
        }

        int fraudCount = 0;
        for (Neighbor n : pq) {
            if (n.isFraud) fraudCount++;
        }

        return (double) fraudCount / k;
    }
}
