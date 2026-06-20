package com.sportytomato.app.service;

import java.util.List;

public class ConflictMergeService {
    public static class LocalData { public int steps, energy, harvestCount, tomatoProgress, tomatoStage; public long energyTimestamp, lastModified; }
    public static class CloudData { public int steps, energy, harvestCount, tomatoProgress, tomatoStage; public long energyTimestamp, lastModified; }
    public static class MergeResult { public boolean conflict; public LocalData resolved; public MergeResult(boolean conflict, LocalData resolved) { this.conflict = conflict; this.resolved = resolved; } }

    public MergeResult merge(LocalData local, CloudData cloud) {
        boolean hasConflict = false; LocalData resolved = new LocalData();
        resolved.steps = Math.max(local.steps, cloud.steps);
        if (resolved.steps != local.steps) hasConflict = true;
        if (local.energyTimestamp >= cloud.energyTimestamp) { resolved.energy = local.energy; resolved.energyTimestamp = local.energyTimestamp; }
        else { resolved.energy = cloud.energy; resolved.energyTimestamp = cloud.energyTimestamp; if (local.energy != cloud.energy) hasConflict = true; }
        resolved.harvestCount = Math.max(local.harvestCount, cloud.harvestCount);
        if (resolved.harvestCount != local.harvestCount) hasConflict = true;
        if (local.tomatoProgress >= cloud.tomatoProgress) { resolved.tomatoProgress = local.tomatoProgress; resolved.tomatoStage = local.tomatoStage; }
        else { resolved.tomatoProgress = cloud.tomatoProgress; resolved.tomatoStage = cloud.tomatoStage; hasConflict = true; }
        resolved.lastModified = Math.max(local.lastModified, cloud.lastModified);
        return new MergeResult(hasConflict, resolved);
    }

    public LocalData lastWriteWins(LocalData local, CloudData cloud) {
        if (local.lastModified >= cloud.lastModified) return local;
        LocalData result = new LocalData(); result.steps = cloud.steps; result.energy = cloud.energy; result.energyTimestamp = cloud.energyTimestamp;
        result.harvestCount = cloud.harvestCount; result.tomatoProgress = cloud.tomatoProgress; result.tomatoStage = cloud.tomatoStage; result.lastModified = cloud.lastModified;
        return result;
    }

    public MergeResult mergeMultiple(LocalData local, List<CloudData> cloudDataList) {
        CloudData mergedCloud = new CloudData();
        if (cloudDataList == null || cloudDataList.isEmpty()) {
            LocalData result = new LocalData(); result.steps = local.steps; result.energy = local.energy; result.energyTimestamp = local.energyTimestamp;
            result.harvestCount = local.harvestCount; result.tomatoProgress = local.tomatoProgress; result.tomatoStage = local.tomatoStage; result.lastModified = local.lastModified;
            return new MergeResult(false, result);
        }
        for (CloudData cd : cloudDataList) {
            mergedCloud.steps = Math.max(mergedCloud.steps, cd.steps);
            if (cd.energyTimestamp > mergedCloud.energyTimestamp) { mergedCloud.energy = cd.energy; mergedCloud.energyTimestamp = cd.energyTimestamp; }
            mergedCloud.harvestCount = Math.max(mergedCloud.harvestCount, cd.harvestCount);
            if (cd.tomatoProgress > mergedCloud.tomatoProgress) { mergedCloud.tomatoProgress = cd.tomatoProgress; mergedCloud.tomatoStage = cd.tomatoStage; }
            if (cd.lastModified > mergedCloud.lastModified) mergedCloud.lastModified = cd.lastModified;
        }
        return merge(local, mergedCloud);
    }
}