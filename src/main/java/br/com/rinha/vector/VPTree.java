package br.com.rinha.vector;

import br.com.rinha.model.Models.Reference;
import java.util.*;

public class VPTree {
    public static class Node {
        public Reference vantagePoint;
        public double threshold;
        public Node left;
        public Node right;

        public Node(Reference vp) {
            this.vantagePoint = vp;
        }
    }

    public static Node build(List<Reference> items) {
        if (items.isEmpty()) return null;

        Reference vp = items.get(0);
        Node node = new Node(vp);
        if (items.size() == 1) return node;

        double[] distances = new double[items.size()];
        for (int i = 0; i < items.size(); i++) {
            distances[i] = Math.sqrt(Vectorizer.euclideanDistanceSq(vp.vector, items.get(i).vector));
        }

        double[] sortedDistances = distances.clone();
        Arrays.sort(sortedDistances);
        double median = sortedDistances[items.size() / 2];
        node.threshold = median;

        List<Reference> leftItems = new ArrayList<>();
        List<Reference> rightItems = new ArrayList<>();

        for (int i = 1; i < items.size(); i++) {
            if (distances[i] < median) {
                leftItems.add(items.get(i));
            } else {
                rightItems.add(items.get(i));
            }
        }

        node.left = build(leftItems);
        node.right = build(rightItems);
        return node;
    }

    public static void search(Node node, double[] target, int k, PriorityQueue<KNN.Neighbor> pq) {
        if (node == null) return;

        double dist = Math.sqrt(Vectorizer.euclideanDistanceSq(target, node.vantagePoint.vector));

        if (pq.size() < k) {
            pq.add(new KNN.Neighbor(dist * dist, "fraud".equals(node.vantagePoint.label)));
        } else if (dist * dist < pq.peek().distSq) {
            pq.poll();
            pq.add(new KNN.Neighbor(dist * dist, "fraud".equals(node.vantagePoint.label)));
        }

        if (node.left == null && node.right == null) return;

        double tau = Math.sqrt(pq.peek().distSq);
        if (pq.size() < k) tau = Double.MAX_VALUE;

        if (dist < node.threshold) {
            if (dist - tau < node.threshold) search(node.left, target, k, pq);
            if (dist + tau >= node.threshold) search(node.right, target, k, pq);
        } else {
            if (dist + tau >= node.threshold) search(node.right, target, k, pq);
            if (dist - tau < node.threshold) search(node.left, target, k, pq);
        }
    }
}
