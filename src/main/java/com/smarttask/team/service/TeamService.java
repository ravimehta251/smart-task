package com.smarttask.team.service;

import com.smarttask.activitylog.service.ActivityLogService;
import com.smarttask.common.exception.BusinessException;
import com.smarttask.common.exception.ResourceNotFoundException;
import com.smarttask.common.response.PagedResponse;
import com.smarttask.organization.entity.Organization;
import com.smarttask.organization.repository.OrganizationRepository;
import com.smarttask.team.dto.TeamMemberSummary;
import com.smarttask.team.dto.TeamRequest;
import com.smarttask.team.dto.TeamResponse;
import com.smarttask.team.entity.Team;
import com.smarttask.team.repository.TeamRepository;
import com.smarttask.user.entity.User;
import com.smarttask.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;

    @Transactional
    public TeamResponse create(TeamRequest request, User currentUser) {
        Organization org = findOrgOrThrow(request.organizationId());

        if (teamRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(request.name(), org.getId())) {
            throw new BusinessException("A team with this name already exists in this organization.");
        }

        User teamLead = null;
        if (request.teamLeadId() != null) {
            teamLead = findUserOrThrow(request.teamLeadId());
        }

        Team team = Team.builder()
                .name(request.name())
                .description(request.description())
                .organization(org)
                .teamLead(teamLead)
                .build();
        teamRepository.save(team);

        activityLogService.log("TEAM_CREATED", "Team", team.getId(),
                currentUser, "Created team: " + team.getName());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse update(String id, TeamRequest request, User currentUser) {
        Team team = findActiveOrThrow(id);

        if (!team.getName().equals(request.name()) &&
                teamRepository.existsByNameAndOrganizationIdAndDeletedAtIsNull(
                        request.name(), team.getOrganization().getId())) {
            throw new BusinessException("A team with this name already exists in this organization.");
        }

        team.setName(request.name());
        team.setDescription(request.description());

        if (request.teamLeadId() != null) {
            team.setTeamLead(findUserOrThrow(request.teamLeadId()));
        }

        teamRepository.save(team);
        activityLogService.log("TEAM_UPDATED", "Team", team.getId(),
                currentUser, "Updated team: " + team.getName());
        return toResponse(team);
    }

    @Transactional
    public void delete(String id, User currentUser) {
        Team team = findActiveOrThrow(id);
        team.softDelete();
        teamRepository.save(team);
        activityLogService.log("TEAM_DELETED", "Team", id,
                currentUser, "Deleted team: " + team.getName());
    }

    @Transactional(readOnly = true)
    public TeamResponse getById(String id) {
        return toResponse(findActiveOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PagedResponse<TeamResponse> getByOrganization(String orgId, String search, Pageable pageable) {
        Page<Team> page = teamRepository.findByOrganization(orgId, search, pageable);
        return PagedResponse.from(page.map(this::toResponse));
    }

    @Transactional
    public TeamResponse addMember(String teamId, String userId, User currentUser) {
        Team team = findActiveOrThrow(teamId);
        User user = findUserOrThrow(userId);

        if (team.getMembers().stream().anyMatch(m -> m.getId().equals(userId))) {
            throw new BusinessException("User is already a member of this team.");
        }

        team.getMembers().add(user);
        teamRepository.save(team);
        activityLogService.log("TEAM_MEMBER_ADDED", "Team", teamId,
                currentUser, "Added member: " + user.getFullName());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse removeMember(String teamId, String userId, User currentUser) {
        Team team = findActiveOrThrow(teamId);
        User user = findUserOrThrow(userId);
        team.getMembers().removeIf(m -> m.getId().equals(userId));
        teamRepository.save(team);
        activityLogService.log("TEAM_MEMBER_REMOVED", "Team", teamId,
                currentUser, "Removed member: " + user.getFullName());
        return toResponse(team);
    }

    // -------------------------------------------------------
    private Team findActiveOrThrow(String id) {
        return teamRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    private Organization findOrgOrThrow(String id) {
        return organizationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
    }

    private User findUserOrThrow(String id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    private TeamResponse toResponse(Team team) {
        Set<TeamMemberSummary> members = team.getMembers().stream()
                .map(u -> TeamMemberSummary.builder()
                        .id(u.getId()).fullName(u.getFullName())
                        .email(u.getEmail()).role(u.getRole()).build())
                .collect(Collectors.toSet());

        TeamMemberSummary lead = team.getTeamLead() != null
                ? TeamMemberSummary.builder()
                        .id(team.getTeamLead().getId())
                        .fullName(team.getTeamLead().getFullName())
                        .email(team.getTeamLead().getEmail())
                        .role(team.getTeamLead().getRole()).build()
                : null;

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .organizationId(team.getOrganization().getId())
                .organizationName(team.getOrganization().getName())
                .teamLead(lead)
                .memberCount(members.size())
                .members(members)
                .createdAt(team.getCreatedAt())
                .build();
    }
}
