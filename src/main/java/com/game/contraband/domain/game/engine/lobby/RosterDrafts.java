package com.game.contraband.domain.game.engine.lobby;

import com.game.contraband.domain.game.player.PlayerProfile;
import com.game.contraband.domain.game.player.TeamRole;
import com.game.contraband.domain.game.player.TeamRoster;
import java.util.List;

public class RosterDrafts {

    private final RosterDraft smugglerDraft;
    private final RosterDraft inspectorDraft;

    private RosterDrafts(RosterDraft smugglerDraft, RosterDraft inspectorDraft) {
        this.smugglerDraft = smugglerDraft;
        this.inspectorDraft = inspectorDraft;
    }

    public static RosterDrafts create(int maxTeamSize) {
        RosterDraft smugglerDraft = RosterDraft.create("밀수꾼 팀", TeamRole.SMUGGLER, maxTeamSize);
        RosterDraft inspectorDraft = RosterDraft.create("검수관 팀", TeamRole.INSPECTOR, maxTeamSize);

        return new RosterDrafts(smugglerDraft, inspectorDraft);
    }

    public void addSmuggler(PlayerProfile profile) {
        smugglerDraft.add(profile);
    }

    public void addInspector(PlayerProfile profile) {
        inspectorDraft.add(profile);
    }

    public void removeSmuggler(Long playerId) {
        smugglerDraft.remove(playerId);
    }

    public void removeInspector(Long playerId) {
        inspectorDraft.remove(playerId);
    }

    public void removePlayer(Long playerId) {
        if (smugglerDraft.hasPlayer(playerId)) {
            smugglerDraft.remove(playerId);
            return;
        }
        if (inspectorDraft.hasPlayer(playerId)) {
            inspectorDraft.remove(playerId);
            return;
        }

        throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
    }

    public PlayerProfile getPlayer(Long playerId) {
        if (smugglerDraft.hasPlayer(playerId)) {
            return smugglerDraft.getPlayer(playerId);
        }
        if (inspectorDraft.hasPlayer(playerId)) {
            return inspectorDraft.getPlayer(playerId);
        }

        throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
    }

    public void toggleTeam(Long playerId) {
        if (smugglerDraft.hasPlayer(playerId)) {
            PlayerProfile currentProfile = smugglerDraft.getPlayer(playerId);

            validateInspectorDraftCapacity();
            smugglerDraft.remove(playerId);

            PlayerProfile newProfile = currentProfile.withTeamRole(TeamRole.INSPECTOR);

            inspectorDraft.add(newProfile);
            return;
        }
        if (inspectorDraft.hasPlayer(playerId)) {
            PlayerProfile currentProfile = inspectorDraft.getPlayer(playerId);

            validateSmugglerDraftCapacity();
            inspectorDraft.remove(playerId);

            PlayerProfile newProfile = currentProfile.withTeamRole(TeamRole.SMUGGLER);

            smugglerDraft.add(newProfile);
            return;
        }

        throw new IllegalArgumentException("로비에 존재하지 않는 플레이어입니다.");
    }

    private void validateInspectorDraftCapacity() {
        if (inspectorDraft.isFull()) {
            throw new IllegalStateException("검사관 팀 인원이 꽉 찼습니다.");
        }
    }

   private void validateSmugglerDraftCapacity() {
        if (smugglerDraft.isFull()) {
            throw new IllegalStateException("밀수꾼 팀 인원이 꽉 찼습니다.");
        }
   }

    public boolean canAddSmuggler(Long playerId) {
        return doesNotHavePlayer(playerId) && smugglerDraft.hasCapacity();
    }

    public boolean canAddInspector(Long playerId) {
        return doesNotHavePlayer(playerId) && inspectorDraft.hasCapacity();
    }

    public boolean hasPlayer(Long playerId) {
        return smugglerDraft.hasPlayer(playerId) || inspectorDraft.hasPlayer(playerId);
    }

    public boolean doesNotHavePlayer(Long playerId) {
        return !hasPlayer(playerId);
    }

    public int totalPlayerCount() {
        return smugglerDraft.players().size() + inspectorDraft.players().size();
    }

    public boolean canResizeTo(int newTeamSize) {
        return newTeamSize >= smugglerDraft.players().size()
                && newTeamSize >= inspectorDraft.players().size();
    }

    public boolean cannotResizeTo(int newTeamSize) {
        return !canResizeTo(newTeamSize);
    }

    public List<PlayerProfile> smugglerPlayers() {
        return smugglerDraft.players();
    }

    public List<PlayerProfile> inspectorPlayers() {
        return inspectorDraft.players();
    }

    public void updateMaxTeamSize(int newMaxTeamSize) {
        smugglerDraft.updateMaxTeamSize(newMaxTeamSize);
        inspectorDraft.updateMaxTeamSize(newMaxTeamSize);
    }

    public TeamRoster smugglerRoster() {
        return smugglerDraft.toRoster();
    }

    public TeamRoster inspectorRoster() {
        return inspectorDraft.toRoster();
    }
}
