package it.unibo.oop.workers02;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a implementations of a matrix version of MultiThreadedListSumClassic.
 * 
 */
public final class MultiThreadedSumMatrix implements SumMatrix {

    private final int nthread;

    /**
     * @param nthread
     *              no. of thread performing the sum.
     */
    public MultiThreadedSumMatrix(final int nthread) {
        this.nthread = nthread;
    }

    @Override
    public double sum(final double[][] matrix) {
        final int size = matrix.length % nthread + matrix.length / nthread;
        /*
         * Build a list of workers
         */
        final List<Worker> workers = new ArrayList<>(nthread);
        for (int start = 0; start < matrix.length; start += size) {
            workers.add(new Worker(matrix, start, size));
        }
        /*
         * Start them
         */
        for (final Worker w: workers) {
            w.start();
        }
        /*
         * Wait for every one of them to finish. This operation is _way_ better done by
         * using barriers and latches, and the whole operation would be better done with
         * futures.
         */
        long sum = 0;
        for (final Worker w: workers) {
            try {
                w.join();
                sum += w.getResult();
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        /*
         * Return the sum
         */
        return sum;
    }

    private static class Worker extends Thread {
        private final double[][] matrix;
        private final int startpos;
        private final int nelem;
        private long res;

        /**
         * Build a new worker.
         *
         * @param matrix
         *            the matrix to sum
         * @param startpos
         *            the initial position for this worker
         * @param nelem
         *            the no. of elems to sum up for this worker
         */
        Worker(final double[][] matrix, final int startpos, final int nelem) {
            super();
            this.matrix = matrix.clone();
            this.startpos = startpos;
            this.nelem = nelem;
        }

        @Override
        @SuppressWarnings("PMD.SystemPrintln")
        public synchronized void run() {
            System.out.println("Working from position " + startpos + " to position " + (startpos + nelem - 1));
            for (int row = startpos; row < matrix.length && row < startpos + nelem; row++) {
                for (int coloumn = 0; coloumn < matrix[0].length; coloumn++) {
                    this.res += this.matrix[row][coloumn];
                }
            }
        }

        /**
         * Returns the result of summing up the integers within the list.
         *
         * @return the sum of every element in the array
         */
        public synchronized long getResult() {
            return this.res;
        }

    }
}
