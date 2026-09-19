package com.tcm.dashboard;

import com.tcm.dashboard.dto.AdminDashboardResponse;
import com.tcm.dashboard.dto.TrainerDashboardResponse;
import java.util.UUID;

public interface DashboardService {

    /** Platform-wide figures for an administrator. */
    AdminDashboardResponse adminSummary();

    /** The same kind of answer for one trainer, scoped to what they teach. */
    TrainerDashboardResponse trainerSummary(UUID trainerId);
}
