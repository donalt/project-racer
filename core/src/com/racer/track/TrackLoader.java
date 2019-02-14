package com.racer.track;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Scanner;

/**
 * TrackLoader handles the loading of tracks from .track files.
 *
 */
public class TrackLoader {
	private static File[] tracks;
	
	/** Returns how many tracks there are. */
	public static int numTracks() {
		return tracks.length;
	}
	
	/**
	 * Loads all of the tracks in the tracks folder.
	 */
	public static void loadTracks() {
		File dir = new File(".." + File.separatorChar + "tracks");
		tracks = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				return filename.endsWith(".track");
			}
		});
		//Gdx.app.log("", dir.getAbsolutePath());
	}
	
	public static String trackName(int trackId) {
		if (trackId >= tracks.length) return "";
		
		String fullName = tracks[trackId].getName();
		// -6 since ".track" is 6 characters. 
		return fullName.substring(0, fullName.length() - 6);
	}
	
	public static Track loadTrack(int trackId) {
		if (trackId >= tracks.length) return null;
		
		Scanner sc;
		try {
			sc = new Scanner(tracks[trackId]);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
		
		// The first two integers should be the amount of laps and segment length.
		int laps = sc.nextInt();
		int segmentLength = sc.nextInt();
		Track track = new Track(laps, segmentLength);
		
		// Load every segment
		while (sc.hasNextInt()) {
			int length = sc.nextInt();
			int width = sc.nextInt();
			int rotation = sc.nextInt();
			track.addSegment(length, width, rotation);
		}
		sc.close();
		
		return track;
	}
}
