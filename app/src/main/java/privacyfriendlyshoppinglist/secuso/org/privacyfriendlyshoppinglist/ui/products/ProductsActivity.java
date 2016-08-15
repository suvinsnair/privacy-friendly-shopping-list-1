package privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.products;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.R;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.framework.context.AbstractInstanceFactory;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.framework.context.InstanceFactory;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.logic.product.business.ProductService;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.logic.product.business.domain.ProductDto;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.logic.product.business.domain.TotalDto;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.logic.shoppingList.business.ShoppingListService;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.logic.shoppingList.business.domain.ListDto;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.main.MainActivity;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.products.listadapter.ProductsAdapter;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.products.listeners.AddProductOnClickListener;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.products.listeners.ShowDeleteProductsOnClickListener;
import privacyfriendlyshoppinglist.secuso.org.privacyfriendlyshoppinglist.ui.products.listeners.SortProductsOnClickListener;

import java.util.List;

/**
 * Description:
 * Author: Grebiel Jose Ifill Brito
 * Created: 09.07.16 creation date
 */
public class ProductsActivity extends AppCompatActivity
{
    private static final long DURATION = 1000L;
    private ProductService productService;
    private ShoppingListService shoppingListService;
    private ProductActivityCache cache;
    private String listId;

    @Override
    protected final void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.products_activity);

        AbstractInstanceFactory instanceFactory = new InstanceFactory(getApplicationContext());
        this.productService = (ProductService) instanceFactory.createInstance(ProductService.class);
        this.shoppingListService = (ShoppingListService) instanceFactory.createInstance(ShoppingListService.class);

        listId = getIntent().getStringExtra(MainActivity.LIST_ID_KEY);
        ListDto dto = shoppingListService.getById(listId);
        setTitle(dto.getListName());

        cache = new ProductActivityCache(this, listId, dto.getListName(), dto.isStatisticEnabled());

        updateListView();

        cache.getNewListFab().setOnClickListener(new AddProductOnClickListener(cache));

        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.lists_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        MenuItem sortItem = menu.findItem(R.id.imageview_sort);
        sortItem.setOnMenuItemClickListener(new SortProductsOnClickListener(cache));

        MenuItem deleteItem = menu.findItem(R.id.imageview_delete);
        deleteItem.setOnMenuItemClickListener(new ShowDeleteProductsOnClickListener(cache));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        updateListView();
    }

    public void updateListView()
    {
        List<ProductDto> allProducts = productService.getAllProducts(cache.getListId());

        // sort according to last sort selection
        ListDto listDto = shoppingListService.getById(listId);
        String sortBy = listDto.getSortCriteria();
        boolean sortAscending = listDto.isSortAscending();
        productService.sortProducts(allProducts, sortBy, sortAscending);

        cache.getProductsAdapter().setProductsList(allProducts);
        cache.getProductsAdapter().notifyDataSetChanged();

        reorderProductViewBySelection();
        updateTotals();
    }

    public void updateTotals()
    {
        TotalDto totalDto = productService.computeTotals(cache.getProductsAdapter().getProductsList());
        cache.getTotalAmountTextView().setText(totalDto.getTotalAmount());
        cache.getTotalCheckedTextView().setText(totalDto.getCheckedAmount());

        if ( totalDto.isEqualsZero() )
        {
            cache.getTotalLayout().animate().alpha(0.0f).setDuration(DURATION);
            cache.getTotalLayout().setVisibility(View.GONE);
        }
        else
        {
            cache.getTotalLayout().setVisibility(View.VISIBLE);
            cache.getTotalLayout().animate().alpha(1.0f).setDuration(DURATION);
        }
    }

    public void changeItemPosition(ProductDto dto)
    {
        ProductsAdapter productsAdapter = cache.getProductsAdapter();
        List<ProductDto> productsList = productsAdapter.getProductsList();
        List<ProductDto> productDtos = productService.moveSelectedToEnd(productsList);
        productsAdapter.setProductsList(productDtos);

        int initialPosition = productsList.indexOf(dto);
        int finalPosition = productDtos.indexOf(dto);
        productsAdapter.notifyItemMoved(initialPosition, finalPosition);
        // Animation ends in final position when the initial position is equals zero.
        // Therefore the animation needs to be fix by scrolling back to position 0.
        if ( initialPosition == 0 )
        {
            cache.getRecyclerView().scrollToPosition(0);
        }
    }

    public void reorderProductViewBySelection()
    {
        ProductsAdapter productsAdapter = cache.getProductsAdapter();
        List<ProductDto> productsList = productsAdapter.getProductsList();
        List<ProductDto> productDtos = productService.moveSelectedToEnd(productsList);
        productsAdapter.setProductsList(productDtos);
        productsAdapter.notifyDataSetChanged();
    }

    public void reorderProductView(List<ProductDto> sortedProducts)
    {
        cache.getProductsAdapter().setProductsList(sortedProducts);
        cache.getProductsAdapter().notifyDataSetChanged();
    }
}
