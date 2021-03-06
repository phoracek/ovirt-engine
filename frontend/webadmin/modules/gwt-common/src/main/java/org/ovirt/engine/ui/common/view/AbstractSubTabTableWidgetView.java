package org.ovirt.engine.ui.common.view;

import org.ovirt.engine.ui.common.idhandler.WithElementId;
import org.ovirt.engine.ui.common.presenter.AbstractSubTabPresenter;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.uicommon.AbstractModelBoundTableWidget;
import org.ovirt.engine.ui.uicommonweb.models.ListWithDetailsModel;
import org.ovirt.engine.ui.uicommonweb.models.SearchableListModel;

import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.IsWidget;

/**
 * Base class for sub tab views that use {@linkplain AbstractModelBoundTableWidget model-bound table widgets}.
 *
 * @param <I>
 *            Main tab table row data type.
 * @param <T>
 *            Sub tab table row data type.
 * @param <M>
 *            Main model type.
 * @param <D>
 *            Detail model type.
 */
public class AbstractSubTabTableWidgetView<I, T, M extends ListWithDetailsModel, D extends SearchableListModel>
    extends AbstractView implements AbstractSubTabPresenter.ViewDef<I> {

    private final AbstractModelBoundTableWidget<T, D> modelBoundTableWidget;

    @WithElementId
    public final SimpleActionTable<T> table;

    public AbstractSubTabTableWidgetView(AbstractModelBoundTableWidget<T, D> modelBoundTableWidget) {
        this.modelBoundTableWidget = modelBoundTableWidget;
        this.table = modelBoundTableWidget.getTable();
    }

    @Override
    public HandlerRegistration addWindowResizeHandler(ResizeHandler handler) {
        return Window.addResizeHandler(handler);
    }

    @Override
    public void resizeToFullHeight() {
        int tableTop = table.getTableAbsoluteTop();
        if (tableTop > 0) {
            table.setMaxGridHeight(Window.getClientHeight() - tableTop);
            table.updateGridSize();
        }
    }

    @Override
    public IsWidget getTableContainer() {
        return modelBoundTableWidget;
    }

    protected AbstractModelBoundTableWidget<T, D> getModelBoundTableWidget() {
        return modelBoundTableWidget;
    }

    @Override
    public SimpleActionTable<T> getTable() {
        return modelBoundTableWidget.getTable();
    }

    protected void initTable() {
        getModelBoundTableWidget().initTable();
    }

    @Override
    public void setMainSelectedItem(I selectedItem) {
        // No-op since table-based sub tab views don't handle main tab selection on their own
    }

}
