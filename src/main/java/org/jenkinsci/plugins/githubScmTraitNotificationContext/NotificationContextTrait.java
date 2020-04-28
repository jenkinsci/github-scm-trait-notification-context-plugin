package org.jenkinsci.plugins.githubScmTraitNotificationContext;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.model.Result;
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
    public NotificationContextTrait(String contextLabel, boolean typeSuffix, String messageGood,
                                    String messageUnstable, String messageFailure, String messageAborted,
                                    String messageOther, String messagePending, String messageQueued) {
        this.contextLabel = contextLabel;
        this.typeSuffix = typeSuffix;
        this.messageGood = messageGood;
        this.messageUnstable = messageUnstable;
        this.messageFailure = messageFailure;
        this.messageAborted = messageAborted;
        this.messageOther = messageOther;
        this.messagePending = messagePending;
        this.messageQueued = messageQueued;
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
        CustomContextNotificationStrategy notificationStrategy = new CustomContextNotificationStrategy(contextLabel, typeSuffix);
        if (!messageGood.isEmpty()) {
            notificationStrategy.messageGood = messageGood;
        }
        if (!messageUnstable.isEmpty()) {
            notificationStrategy.messageUnstable = messageUnstable;
        }
        if (!messageFailure.isEmpty()) {
            notificationStrategy.messageFailure = messageFailure;
        }
        if (!messageAborted.isEmpty()) {
            notificationStrategy.messageAborted = messageAborted;
        }
        if (!messageOther.isEmpty()) {
            notificationStrategy.messageOther = messageOther;
        }
        if (!messagePending.isEmpty()) {
            notificationStrategy.messagePending = messagePending;
        }
        if (!messageQueued.isEmpty()) {
            notificationStrategy.messageQueued = messageQueued;
        }
        githubContext.withNotificationStrategies(Collections.singletonList(notificationStrategy));
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
        public String messageGood;
        public String messageUnstable;
        public String messageFailure;
        public String messageAborted;
        public String messageOther;
        public String messagePending;
        public String messageQueued;

        CustomContextNotificationStrategy(String contextLabel, boolean typeSuffix) {
            this.contextLabel = contextLabel;
            this.typeSuffix = typeSuffix;
            this.messageGood = Messages.GitHubBuildStatusNotification_CommitStatus_Good();
            this.messageUnstable = Messages.GitHubBuildStatusNotification_CommitStatus_Unstable();
            this.messageFailure = Messages.GitHubBuildStatusNotification_CommitStatus_Failure();
            this.messageAborted = Messages.GitHubBuildStatusNotification_CommitStatus_Aborted();
            this.messageOther = Messages.GitHubBuildStatusNotification_CommitStatus_Other();
            this.messagePending = Messages.GitHubBuildStatusNotification_CommitStatus_Pending();
            this.messageQueued = Messages.GitHubBuildStatusNotification_CommitStatus_Queued();
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
            String message = this.messageQueued;
            Run<?, ?> build = notificationContext.getBuild();
            if (null != build) {
                Result result = build.getResult();
                if (Result.SUCCESS.equals(result)) {
                    message = this.messageGood;
                } else if (Result.UNSTABLE.equals(result)) {
                    message = this.messageUnstable;
                } else if (Result.FAILURE.equals(result)) {
                    message = this.messageFailure;
                } else if (Result.ABORTED.equals(result)) {
                    message = this.messageAborted;
                } else if (result != null) { // NOT_BUILT etc.
                    message = this.messageOther;
                } else {
                    message = this.messagePending;
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
