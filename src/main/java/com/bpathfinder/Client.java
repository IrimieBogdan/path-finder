package com.bpathfinder;

import com.bpathfinder.dto.TracePoint;
import com.bpathfinder.dto.TrackingRecord;
import com.bpathfinder.parsers.CsvParser;
import com.bpathfinder.parsers.ObjectsToCsv;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Client {
    private static List<TracePoint> pathTraces = new ArrayList<TracePoint>();

    public static void main(String[] args) {
        // parse CSV
        CsvParser csvParser = new CsvParser();
        List<TrackingRecord> trackingRecords = csvParser.parse();

        // flatten time
        Client client = new Client();
        client.flattenTime(trackingRecords);

        pathTraces.stream().forEach(System.out::println);

        ObjectsToCsv objectsToCsv = new ObjectsToCsv();
        objectsToCsv.writePojoToCsv(pathTraces);
    }


    public void flattenTime(List<TrackingRecord> trackingRecords) {
        List<TrackingRecord> flatTime = new ArrayList<TrackingRecord>();

        // the idea is to consider event that happen in 1 second interval as happening at the same time
        trackingRecords.stream().forEach( trackingRecord -> {
            if (flatTime.size() >= 1) {
                if (flatTime.get(0).getTime() + 1 > trackingRecord.getTime()) {
                    flatTime.add(trackingRecord);
                }
                else {
                    //computeTrackingPointByAverage(flatTime);
                    //computeTracePointByTrilateration(flatTime);
                    if (flatTime.stream().distinct().count() >= 3) {
                        TracePoint tp = computeTracePointByTrilateration(flatTime);
                        pathTraces.add(tp);
                    }


                    while (flatTime.size() > 0 && flatTime.get(0).getTime() + 1 < trackingRecord.getTime()) {
                        flatTime.remove(0);
                    }
                    flatTime.add(trackingRecord);
                }
            }
            else {
                flatTime.add(trackingRecord);
            }
            // System.out.println("Time flattened!");
        });
    }

    private void computeTrackingPointByAverage(List<TrackingRecord> trackingRecords) {

        if (trackingRecords.stream().distinct().count() >= 3) {
            double x = 0, y = 0, time = 0;
            int counter = 0;

            Iterator<TrackingRecord> distinctPoints = trackingRecords.stream().distinct().collect(Collectors.toList()).iterator();
            while (distinctPoints.hasNext()) {
                TrackingRecord tr = distinctPoints.next();
                x += tr.getxAxis();
                y += tr.getyAxis();
                time += tr.getTime();
                counter++;
            };

            pathTraces.add(new TracePoint(time/counter, x/counter, y/counter));

        }
    }

    private TracePoint computeTracePointByTrilateration(List<TrackingRecord> trackingRecords) {
        double[][] positions = new double[trackingRecords.size()][2];
        double[] distances = new double[trackingRecords.size()];
        int recordCounter = 0;
        double time = 0;

        Iterator<TrackingRecord> it = trackingRecords.iterator();
        while (it.hasNext()) {
            TrackingRecord tr = it.next();
            positions[recordCounter][0] = tr.getxAxis();
            positions[recordCounter][1] = tr.getyAxis();
            distances[recordCounter++] = tr.getRssi();
            time += tr.getTime();
        }

        NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
        LeastSquaresOptimizer.Optimum optimum = solver.solve();
        double[] centroid = optimum.getPoint().toArray();

        return new TracePoint(time/recordCounter, centroid[0], centroid[1]);
    }
}
