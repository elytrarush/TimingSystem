package me.makkuusen.timing.system.replay;

import me.jumper251.replay.filesystem.saving.IReplaySaver;
import me.jumper251.replay.filesystem.saving.DefaultReplaySaver;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.data.ReplayData;
import me.jumper251.replay.utils.fetcher.Consumer;
import me.makkuusen.timing.system.TimingSystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TimingSystemReplaySaver implements IReplaySaver {

    private final File baseDirectory;
    private final IReplaySaver delegate = new DefaultReplaySaver();

    public TimingSystemReplaySaver(File dataFolder) {
        this.baseDirectory = new File(dataFolder, "replays");
    }

    @Override
    public void saveReplay(Replay replay) {
        Optional<ReplayIntegration.AttemptContext> contextOptional = ReplayIntegration.getInstance().pollContext(replay.getId());

        if (contextOptional.isEmpty()) {
            delegate.saveReplay(replay);
            return;
        }

        ReplayIntegration.AttemptContext context = contextOptional.get();
        if (!context.personalBest()) {
            // We recorded the attempt purely for analytics – nothing to persist.
            return;
        }

        File targetFile = resolveTargetFile(context);
        ensureDirectory(targetFile.getParentFile());

        try (FileOutputStream fos = new FileOutputStream(targetFile);
             GZIPOutputStream gos = new GZIPOutputStream(fos);
             ObjectOutputStream oos = new ObjectOutputStream(gos)) {
            oos.writeObject(replay.getData());
        } catch (IOException ioException) {
            TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to save replay " + replay.getId(), ioException);
        }

        // Store a tiny metadata file alongside the replay for quick lookups.
        writeMetadataFile(context, targetFile.toPath());
    }

    @Override
    public boolean replayExists(String replayName) {
        if (Files.exists(resolveTargetPath(replayName))) {
            return true;
        }
        return delegate.replayExists(replayName);
    }

    @Override
    public void loadReplay(String replayName, Consumer<Replay> consumer) {
        Path replayPath = resolveTargetPath(replayName);
        if (Files.exists(replayPath)) {
            try (FileInputStream fis = new FileInputStream(replayPath.toFile());
                 GZIPInputStream gis = new GZIPInputStream(fis);
                 ObjectInputStream ois = new ObjectInputStream(gis)) {
                ReplayData data = (ReplayData) ois.readObject();
                consumer.accept(new Replay(replayName, data));
                return;
            } catch (IOException | ClassNotFoundException exception) {
                TimingSystem.getPlugin().getLogger().log(Level.SEVERE, "Failed to load replay " + replayName, exception);
            }
        }
        delegate.loadReplay(replayName, consumer);
    }

    @Override
    public void deleteReplay(String replayName) {
        Path replayPath = resolveTargetPath(replayName);
        try {
            Files.deleteIfExists(replayPath);
            Files.deleteIfExists(replayPath.resolveSibling(replayName + ".meta"));
        } catch (IOException exception) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to delete replay " + replayName, exception);
        }
        delegate.deleteReplay(replayName);
    }

    @Override
    public List<String> getReplays() {
        if (!baseDirectory.exists()) {
            return delegate.getReplays();
        }
        File[] files = baseDirectory.listFiles((dir, name) -> name.endsWith(".replay"));
        if (files == null || files.length == 0) {
            return delegate.getReplays();
        }
        return java.util.Arrays.stream(files)
            .map(file -> file.getName().substring(0, file.getName().length() - ".replay".length()))
            .toList();
    }

    private void ensureDirectory(File directory) {
        if (!directory.exists() && !directory.mkdirs()) {
            TimingSystem.getPlugin().getLogger().warning("Could not create replay directory: " + directory.getAbsolutePath());
        }
    }

    private File resolveTargetFile(ReplayIntegration.AttemptContext context) {
        String fileName = context.storageFileName() + ".replay";
        return new File(baseDirectory, fileName);
    }

    private Path resolveTargetPath(String replayName) {
        return new File(baseDirectory, replayName + ".replay").toPath();
    }

    private void writeMetadataFile(ReplayIntegration.AttemptContext context, Path replayPath) {
        String metadata = "player=" + context.playerName() + System.lineSeparator()
            + "playerId=" + context.playerId() + System.lineSeparator()
            + "trackId=" + context.trackId() + System.lineSeparator()
            + "trackName=" + context.trackName() + System.lineSeparator()
            + "finishTime=" + context.finishTime() + System.lineSeparator()
            + "recordedAt=" + context.startedAt();
        Path metadataPath = replayPath.resolveSibling(context.storageFileName() + ".meta");
        try {
            Files.writeString(metadataPath, metadata);
        } catch (IOException exception) {
            TimingSystem.getPlugin().getLogger().log(Level.WARNING, "Failed to store metadata for replay " + context.storageFileName(), exception);
        }
    }
}
