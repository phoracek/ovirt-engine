package org.ovirt.engine.core.bll;

import java.util.List;

import org.ovirt.engine.core.bll.job.ExecutionHandler;
import org.ovirt.engine.core.bll.tasks.CommandCoordinatorUtil;
import org.ovirt.engine.core.bll.tasks.interfaces.CommandCallback;
import org.ovirt.engine.core.common.action.VdcActionType;
import org.ovirt.engine.core.common.businessentities.CommandEntity;
import org.ovirt.engine.core.compat.CommandStatus;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.compat.backendcompat.CommandExecutionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChildCommandsCallbackBase extends CommandCallback {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void doPolling(Guid cmdId, List<Guid> childCmdIds) {
        CommandExecutionStatus status = CommandCoordinatorUtil.getCommandExecutionStatus(cmdId);
        // TODO: should be removed when doPolling will be moved to run only after execute finish - here for test purpose
        // only.
        if (status != CommandExecutionStatus.EXECUTED &&
                CommandCoordinatorUtil.getCommandStatus(cmdId) == CommandStatus.ACTIVE) {
            return;
        }

        boolean anyFailed = false;
        int completedChildren = 0;
        CommandBase<?> command = getCommand(cmdId);
        for (Guid childCmdId : childCmdIds) {
            CommandBase<?> child = getCommand(childCmdId);
            switch (CommandCoordinatorUtil.getCommandStatus(childCmdId)) {
            case NOT_STARTED:
            case ACTIVE:
                log.info("Waiting on child command id: '{}' type:'{}' of '{}' (id: '{}') to complete",
                        childCmdId,
                        child.getActionType(),
                        command.getActionType(),
                        cmdId);
                return;
            case FAILED:
            case FAILED_RESTARTED:
            case UNKNOWN:
                anyFailed = true;
                break;
            default:
                CommandEntity cmdEntity = CommandCoordinatorUtil.getCommandEntity(childCmdId);
                if (cmdEntity.isCallbackNotified() || !cmdEntity.isCallbackEnabled()) {
                    ++completedChildren;
                    break;
                } else {
                    // log.info("command '{}' id: '{}' is waiting for child command(s) '{}' to complete",
                    // command.getActionType(), cmdId, childCmdIds);
                    log.info("Waiting on child command id: '{}' type:'{}' of '{}' (id: '{}') to complete",
                            childCmdId,
                            child.getActionType(),
                            command.getActionType(),
                            cmdId);
                    return;
                }
            }
        }

        childCommandsExecutionEnded(command, anyFailed, childCmdIds, status, completedChildren);
    }

    protected abstract void childCommandsExecutionEnded(CommandBase<?> command,
            boolean anyFailed,
            List<Guid> childCmdIds,
            CommandExecutionStatus status,
            int completedChildren);

    private void endAction(CommandBase<?> commandBase, List<Guid> childCmdIds, boolean succeeded) {
        if (commandBase.getParameters().getParentCommand() == VdcActionType.Unknown
                || !commandBase.getParameters().getShouldBeEndedByParent()) {

            commandBase.endAction();

            if (commandBase.getParameters().getParentCommand() == VdcActionType.Unknown) {
                CommandCoordinatorUtil.removeAllCommandsInHierarchy(commandBase.getCommandId());
            }

            ExecutionHandler.endJob(commandBase.getExecutionContext(), succeeded);
        }
    }

    @Override
    public void onSucceeded(Guid cmdId, List<Guid> childCmdIds) {
        endAction(getCommand(cmdId), childCmdIds, true);
    }

    @Override
    public void onFailed(Guid cmdId, List<Guid> childCmdIds) {
        CommandBase<?> commandBase = getCommand(cmdId);
        // This should be removed as soon as infra bug will be fixed and failed execution will reach endWithFailure
        commandBase.getParameters().setTaskGroupSuccess(false);
        endAction(commandBase, childCmdIds, false);
    }

    protected CommandBase<?> getCommand(Guid cmdId) {
        return CommandCoordinatorUtil.retrieveCommand(cmdId);
    }
}
