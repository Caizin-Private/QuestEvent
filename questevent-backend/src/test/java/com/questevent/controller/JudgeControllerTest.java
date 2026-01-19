package com.questevent.controller;

import com.questevent.dto.JudgeSubmissionDTO;
import com.questevent.dto.JudgeSubmissionDetailsDTO;
import com.questevent.dto.JudgeSubmissionStatsDTO;
import com.questevent.rbac.RbacService;
import com.questevent.service.JudgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JudgeController.class)
class JudgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JudgeService judgeService;

    @MockBean
    private RbacService rbacService;

    @Test
    @WithMockUser
    void getPendingSubmissions_ownerAllowed_returns200() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(judgeService.getPendingSubmissionsForJudge(any()))
                .thenReturn(List.of(mock(JudgeSubmissionDTO.class)));

        mockMvc.perform(get("/api/judge/submissions/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getPendingSubmissionsForActivity_ownerAllowed_returns200() throws Exception {
        UUID activityId = UUID.randomUUID();

        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(judgeService.getPendingSubmissionsForActivity(eq(activityId), any()))
                .thenReturn(List.of(mock(JudgeSubmissionDTO.class)));

        mockMvc.perform(
                        get("/api/judge/submissions/pending/activity/{activityId}", activityId)
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAllSubmissions_ownerAllowed_returns200() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(judgeService.getAllSubmissionsForJudge(any()))
                .thenReturn(List.of(mock(JudgeSubmissionDTO.class)));

        mockMvc.perform(get("/api/judge/submissions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void reviewSubmission_ownerAllowed_returns200() throws Exception {
        UUID submissionId = UUID.randomUUID();

        when(rbacService.isPlatformOwner(any())).thenReturn(true);

        mockMvc.perform(
                        patch("/api/judge/submissions/{submissionId}/review", submissionId)
                                .with(csrf())
                )
                .andExpect(status().isOk());

        verify(judgeService).reviewSubmission(submissionId);
    }

    @Test
    @WithMockUser
    void rejectSubmission_ownerAllowed_returns200() throws Exception {
        UUID submissionId = UUID.randomUUID();

        when(rbacService.isPlatformOwner(any())).thenReturn(true);

        mockMvc.perform(
                        patch("/api/judge/submissions/{submissionId}/reject", submissionId)
                                .with(csrf())
                )
                .andExpect(status().isOk());

        verify(judgeService).rejectSubmission(eq(submissionId), any());
    }

    @Test
    @WithMockUser
    void getSubmissionDetails_ownerAllowed_returns200() throws Exception {
        UUID submissionId = UUID.randomUUID();

        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(judgeService.getSubmissionDetails(eq(submissionId), any()))
                .thenReturn(mock(JudgeSubmissionDetailsDTO.class));

        mockMvc.perform(
                        get("/api/judge/submissions/{submissionId}", submissionId)
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getSubmissionStats_ownerAllowed_returns200() throws Exception {
        when(rbacService.isPlatformOwner(any())).thenReturn(true);
        when(judgeService.getSubmissionStats(any()))
                .thenReturn(mock(JudgeSubmissionStatsDTO.class));

        mockMvc.perform(get("/api/judge/submissions/stats"))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticated_accessDenied() throws Exception {
        mockMvc.perform(get("/api/judge/submissions"))
                .andExpect(status().isUnauthorized());
    }
}
