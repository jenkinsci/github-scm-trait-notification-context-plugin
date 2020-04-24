package org.jenkinsci.plugins.githubScmTraitNotificationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class NotificationContextTrait extends SCMSourceTrait {

    private String contextLabel;
    private boolean typeSuffix;
    private String messageGood;
    private String messageUnstable;
    private String messageFailure;
    private String messageAborted;
    private String messageOther;
    private String messagePending;
    private String messageQueued;

    @DataBoundConstructor
    public NotificationContextTrait(String contextLabel, boolean typeSuffix) {
        this.contextLabel = contextLabel;
        this.typeSuffix = typeSuffix;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public boolean isTypeSuffix() {
        return typeSuffix;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext githubContext = (GitHubSCMSourceContext) context;
        githubContext.withNotificationStrategies(Collections.singletonList(
                new CustomContextNotificationStrategy(contextLabel, typeSuffix)));
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

        CustomContextNotificationStrategy(String contextLabel, boolean typeSuffix) {
            this.contextLabel = contextLabel;
            this.typeSuffix = typeSuffix;
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
            message = messageQueued.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Queued() : messageQueued;
            build = notificationContext.getBuild();
            if (null != build) {
                Result result = build.getResult();
                if (Result.SUCCESS.equals(result)) {
                    return messageGood.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Good() : messageGood;
                } else if (Result.UNSTABLE.equals(result)) {
                    return messageUnstable.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Unstable() : messageUnstable;
                } else if (Result.FAILURE.equals(result)) {
                    return messageFailure.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Failure() : messageFailure;
                } else if (Result.ABORTED.equals(result)) {
                    return messageAborted.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Aborted() : messageAborted;
                } else if (result != null) { // NOT_BUILT etc.
                    return messageOther.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Other() : messageOther;
                } else {
                    return messagePending.isEmpty() ? Messages.GitHubBuildStatusNotification_CommitStatus_Pending() : messagePending;
                }
            }

            return Collections.singletonList(GitHubNotificationRequest.build(buildContext(notificationContext),
                    notificationContext.getDefaultUrl(listener),
                    message,
                    notificationContext.getDefaultState(listener),
                    notificationContext.getDefaultIgnoreError(listener)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomContextNotificationStrategy that = (CustomContextNotificationStrategy) o;
            return typeSuffix == that.typeSuffix &&
                    Objects.equals(contextLabel, that.contextLabel);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextLabel, typeSuffix);
        }
    }
}
