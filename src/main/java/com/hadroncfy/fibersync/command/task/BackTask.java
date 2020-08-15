package com.hadroncfy.fibersync.command.task;

import com.hadroncfy.fibersync.backup.BackupEntry;
import com.hadroncfy.fibersync.command.FileOperationProgressBar;
import com.hadroncfy.fibersync.interfaces.IServer;

import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.hadroncfy.fibersync.FibersyncMod.getConfig;
import static com.hadroncfy.fibersync.config.TextRenderer.render;

import java.util.concurrent.CompletableFuture;

import static com.hadroncfy.fibersync.FibersyncMod.getFormat;

public class BackTask extends BaseTask {
    private final BackupEntry selected;
    private final BackupEntry currentWorld;
    private BackupEntry autoBackup;

    public BackTask(ServerCommandSource src, BackupEntry selected) {
        super(src);
        this.selected = selected;
        currentWorld = cctx.getBackupFactory().create(getConfig().oldWorldName, getConfig().oldWorldDescription, getSourceUUID(src));
        currentWorld.getInfo().locked = true;
        currentWorld.getInfo().isOldWorld = true;
    }
    
    private void afterBack(){
        server.getPlayerManager().broadcastChatMessage(getFinishedText(), true);
        if (autoBackup != currentWorld){
            LOGGER.info("Copying file back from temp dir");
            CompletableFuture.runAsync(() -> {
                FileOperationProgressBar progressBar = new FileOperationProgressBar(server, getFormat().fileCopyBarTitle);
                try {
                    autoBackup.copyTo(currentWorld, progressBar);
                    server.getPlayerManager().broadcastChatMessage(getFormat().copiedFromTempDir, false);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    server.getPlayerManager().broadcastChatMessage(render(getFormat().failedToCopyFromTempDir, e1.toString()), false);
                }
                finally {
                    progressBar.done();
                    cctx.endTask();
                }
            });
        }
        else {
            cctx.endTask();
        }
    }

    private void startBack(Boolean b){
        if (b){
            server.getPlayerManager().broadcastChatMessage(getStartedText(), true);
            ((IServer) server).reloadAll(selected, this::afterBack);
        }
        else {
            cctx.endTask();
        }
    }

    private void prepareToBack(){
        cctx.createCountDownTask(i -> {
            Text txt = render(getCountDownTitleText(), Integer.toString(i));
            server.getPlayerManager().sendToAll(new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, txt, 10, 10, -1));
        }).thenAccept(this::startBack);
    }

    private void runBackTask(ServerCommandSource dummy){
        if (cctx.tryBeginTask(src)){
            server.getPlayerManager().broadcastChatMessage(render(getStartAlertText(), src.getName(), selected.getInfo().name), true);
            
            autoBackup = currentWorld;
            if (selected.collides(currentWorld)){
                LOGGER.info("Backup to temp dir since we are rolling back to oldworld");
                autoBackup = autoBackup.createAtNewDir(getConfig().tempDir);
            }

            doBackup(autoBackup).thenRun(this::prepareToBack)
            .exceptionally(e -> {
                server.getPlayerManager().broadcastChatMessage(render(getFailedText(), src.getName(), e.toString()), false);
                cctx.endTask();
                return null;
            });
        }
    }

    public int run(){
        cctx.getConfirmationManager().submit(src.getName(), src, this::runBackTask);
        return 0;
    }

    protected Text getCountDownTitleText(){
        return getFormat().countDownTitle;
    }
    protected Text getStartAlertText(){
        return getFormat().rollbackConfirmedAlert;
    }
    protected Text getFailedText(){
        return getFormat().backupFailed;
    }
    protected Text getStartedText(){
        return getFormat().rollbackStarted;
    }
    protected Text getFinishedText(){
        return getFormat().rollbackFinished;
    }
}