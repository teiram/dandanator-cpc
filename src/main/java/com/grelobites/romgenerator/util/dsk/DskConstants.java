package com.grelobites.romgenerator.util.dsk;

public class DskConstants {

    public static final FileSystemParameters ROMSET_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(232)
                    .withBlockSize(2048)
                    .withDirectoryEntries(128)
                    .withSectorsByTrack(9)
                    .withTrackCount(80)
                    .withSectorSize(512)
                    .build();

    public static final FileSystemParameters PLUS3_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(175)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(1)
                    .build();

    public static final FileSystemParameters CPC_SYSTEM_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(180)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(2)
                    .build();

    public static final FileSystemParameters CPC_DATA_FS_PARAMETERS =
            FileSystemParameters.newBuilder()
                    .withBlockCount(180)
                    .withBlockSize(1024)
                    .withDirectoryEntries(64)
                    .withSectorsByTrack(9)
                    .withTrackCount(40)
                    .withSectorSize(512)
                    .withReservedTracks(0)
                    .build();

}
