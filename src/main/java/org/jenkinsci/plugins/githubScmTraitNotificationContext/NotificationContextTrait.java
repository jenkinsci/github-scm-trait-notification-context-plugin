package org.jenkinsci.plugins.githubScmTraitNotificationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Result;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.SCMHeadCategory;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.trait.SCMBuilder;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceTrait;
import jenkins.scm.api.trait.SCMSourceTraitDescriptor;
import org.jenkinsci.plugins.github_branch_source.*;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotificationContextTrait extends SCMSourceTrait {

    private String contextLabel;
    private boolean typeSuffix;
    private ReportedNotifications reported;

    @DataBoundConstructor
    public NotificationContextTrait(String contextLabel, boolean typeSuffix, boolean reportSuccess, boolean reportUnstable,
                                    boolean reportFailure, boolean reportNotBuilt, boolean reportAborted) {
        this.contextLabel = contextLabel;
        this.typeSuffix = typeSuffix;
        this.reported = new ReportedNotifications(reportSuccess, reportUnstable, reportFailure, reportNotBuilt,
                reportAborted);
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public boolean isTypeSuffix() {
        return typeSuffix;
    }

    public boolean isReportSuccess() {
        return reported.reportSuccess;
    }

    public boolean isReportUnstable() {
        return reported.reportUnstable;
    }

    public boolean isReportFailure() {
        return reported.reportFailure;
    }

    public boolean isReportNotBuilt() {
        return reported.reportNotBuilt;
    }

    public boolean isReportAborted() {
        return reported.reportAborted;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext githubContext = (GitHubSCMSourceContext) context;
        githubContext.withNotificationStrategies(Collections.singletonList(
                new CustomContextNotificationStrategy(contextLabel, typeSuffix, reported)));
    }

    @Override
    public boolean includeCategory(@NonNull SCMHeadCategory category) {
        return category.isUncategorized();
    }

    @Extension
    public static class DescriptorImpl extends SCMSourceTraitDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Custom Github Notification Context";
        }

        @Override
        public Class<? extends SCMBuilder> getBuilderClass() {
            return GitHubSCMBuilder.class;
        }

        @Override
        public Class<? extends SCMSourceContext> getContextClass() {
            return GitHubSCMSourceContext.class;
        }

        @Override
        public Class<? extends SCMSource> getSourceClass() {
            return GitHubSCMSource.class;
        }
    }

    private static final class CustomContextNotificationStrategy extends AbstractGitHubNotificationStrategy {

        private String contextLabel;
        private boolean typeSuffix;
        private ReportedNotifications reported;

        CustomContextNotificationStrategy(String contextLabel, boolean typeSuffix, ReportedNotifications reported) {
            this.contextLabel = contextLabel;
            this.typeSuffix = typeSuffix;
            this.reported = reported;
        }

        private String buildContext(GitHubNotificationContext notificationContext) {
            SCMHead head = notificationContext.getHead();
            if (typeSuffix) {
                if (head instanceof PullRequestSCMHead) {
                    if (((PullRequestSCMHead) head).isMerge()) {
                        return contextLabel + "/pr-merge";
                    } else {
                        return contextLabel + "/pr-head";
                    }
                } else {
                    return contextLabel + "/branch";
                }
            }
            return contextLabel;
        }

        @Override
        public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
            Result buildResult = notificationContext.getBuild().getResult();
            if (!shouldReport(buildResult)) {
                return new ArrayList<>();
            }
            return Collections.singletonList(GitHubNotificationRequest.build(buildContext(notificationContext),
                    notificationContext.getDefaultUrl(listener),
                    notificationContext.getDefaultMessage(listener),
                    notificationContext.getDefaultState(listener),
                    notificationContext.getDefaultIgnoreError(listener)));
        }

        private boolean shouldReport(Result buildResult) {
            if (buildResult == null) {
                return true;
            }
            switch (buildResult.toString()) {
                case "ABORTED":
                    return reported.reportAborted;
                case "SUCCESS":
                    return reported.reportSuccess;
                case "UNSTABLE":
                    return reported.reportUnstable;
                case "FAILURE":
                    return reported.reportFailure;
                case "NOT_BUILT":
                    return reported.reportNotBuilt;
                default:
                    return true;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomContextNotificationStrategy that = (CustomContextNotificationStrategy) o;
            return typeSuffix == that.typeSuffix &&
                    Objects.equals(contextLabel, that.contextLabel) &&
                    Objects.equals(reported, that.reported);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextLabel, typeSuffix, reported);
        }
    }

    public static class ReportedNotifications {
        private final boolean reportSuccess;
        private final boolean reportUnstable;
        private final boolean reportFailure;
        private final boolean reportNotBuilt;
        private final boolean reportAborted;

        public ReportedNotifications(boolean reportSuccess, boolean reportUnstable, boolean reportFailure, boolean reportNotBuilt,
                                     boolean reportAborted) {
            this.reportSuccess = reportSuccess;
            this.reportUnstable = reportUnstable;
            this.reportFailure = reportFailure;
            this.reportNotBuilt = reportNotBuilt;
            this.reportAborted = reportAborted;
        }

        public boolean isReportSuccess() {
            return reportSuccess;
        }

        public boolean isReportUnstable() {
            return reportUnstable;
        }

        public boolean isReportFailure() {
            return reportFailure;
        }

        public boolean isReportNotBuilt() {
            return reportNotBuilt;
        }

        public boolean isReportAborted() {
            return reportAborted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ReportedNotifications that = (ReportedNotifications) o;
            return reportSuccess == that.reportSuccess &&
                    reportUnstable == that.reportUnstable &&
                    reportFailure == that.reportFailure &&
                    reportNotBuilt == that.reportNotBuilt &&
                    reportAborted == that.reportAborted;
        }

        @Override
        public int hashCode() {
            return Objects.hash(reportSuccess, reportUnstable, reportFailure, reportNotBuilt, reportAborted);
        }
    }
}
