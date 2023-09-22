package org.jenkinsci.plugins.githubScmTraitNotificationContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github_branch_source.AbstractGitHubNotificationStrategy;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationContext;
import org.jenkinsci.plugins.github_branch_source.GitHubNotificationRequest;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMBuilder;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSource;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceContext;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.kohsuke.stapler.DataBoundConstructor;

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

public class NotificationContextTrait extends SCMSourceTrait {

    private String contextLabel;
    private boolean typeSuffix;
    private boolean multipleStatuses;
    private String multipleStatusDelimiter;

    @DataBoundConstructor
    public NotificationContextTrait(String contextLabel, boolean typeSuffix, boolean multipleStatuses, String multipleStatusDelimiter) {
        this.contextLabel = contextLabel;
        this.typeSuffix = typeSuffix;
        this.multipleStatuses = multipleStatuses;
        this.multipleStatusDelimiter = multipleStatusDelimiter;
    }

    public String getContextLabel() {
        return contextLabel;
    }

    public boolean isTypeSuffix() {
        return typeSuffix;
    }

    public boolean isMultipleStatuses() {
        return multipleStatuses;
    }

    public String getMultipleStatusDelimiter() {
        return multipleStatusDelimiter;
    }

    @Override
    protected void decorateContext(SCMSourceContext<?, ?> context) {
        GitHubSCMSourceContext githubContext = (GitHubSCMSourceContext) context;
        githubContext.withNotificationStrategies(Collections.singletonList(
                new CustomContextNotificationStrategy(contextLabel, typeSuffix, multipleStatuses, multipleStatusDelimiter)));
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
        private boolean multipleStatuses;
        private String multipleStatusDelimiter;

        CustomContextNotificationStrategy(String contextLabel, boolean typeSuffix, boolean multipleStatuses, String multipleStatusDelimiter) {
            this.contextLabel = contextLabel;
            this.typeSuffix = typeSuffix;
            this.multipleStatuses = multipleStatuses;
            this.multipleStatusDelimiter = multipleStatusDelimiter;
        }

        private String buildContext(GitHubNotificationContext notificationContext, String label) {
            SCMHead head = notificationContext.getHead();
            if (typeSuffix) {
                if (head instanceof PullRequestSCMHead) {
                    if (((PullRequestSCMHead) head).isMerge()) {
                        return label + "/pr-merge";
                    } else {
                        return label + "/pr-head";
                    }
                } else {
                    return label + "/branch";
                }
            }
            return label;
        }

        private GitHubNotificationRequest buildNotification(GitHubNotificationContext notificationContext, TaskListener listener, String label) {
            return GitHubNotificationRequest.build(buildContext(notificationContext, label),
                    notificationContext.getDefaultUrl(listener),
                    notificationContext.getDefaultMessage(listener),
                    notificationContext.getDefaultState(listener),
                    notificationContext.getDefaultIgnoreError(listener));
        }

        @Override
        public List<GitHubNotificationRequest> notifications(GitHubNotificationContext notificationContext, TaskListener listener) {
            List<GitHubNotificationRequest> notifications = new ArrayList<>();
            if (this.multipleStatuses && StringUtils.isNotBlank(multipleStatusDelimiter)) {
                String[] contextLabels = StringUtils.split(contextLabel, multipleStatusDelimiter);
                for (String label : contextLabels) {
                    label = StringUtils.trim(label);
                    if (StringUtils.isNotBlank(label)) {
                        notifications.add(buildNotification(notificationContext, listener, label));
                    }
                }
            } else {
                notifications.add(buildNotification(notificationContext, listener, contextLabel));
            }
            return Collections.unmodifiableList(notifications);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomContextNotificationStrategy that = (CustomContextNotificationStrategy) o;
            return Objects.equals(contextLabel, that.contextLabel) && typeSuffix == that.typeSuffix
                    && multipleStatuses == that.multipleStatuses && Objects.equals(multipleStatusDelimiter, that.multipleStatusDelimiter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(contextLabel, typeSuffix, multipleStatuses, multipleStatusDelimiter);
        }
    }
}
