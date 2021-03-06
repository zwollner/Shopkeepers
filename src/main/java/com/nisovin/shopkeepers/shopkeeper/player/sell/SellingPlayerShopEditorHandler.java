package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;

public class SellingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected SellingPlayerShopEditorHandler(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected List<TradingRecipeDraft> getTradingRecipes() {
		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		List<TradingRecipeDraft> recipes = new ArrayList<>();

		// add the shopkeeper's offers:
		for (PriceOffer offer : shopkeeper.getOffers()) {
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(offer.getItem(), offer.getPrice());
			recipes.add(recipe);
		}

		// add empty offers for items from the chest:
		List<ItemCount> chestItems = shopkeeper.getItemsFromChest();
		for (int chestItemIndex = 0; chestItemIndex < chestItems.size(); chestItemIndex++) {
			ItemCount itemCount = chestItems.get(chestItemIndex);
			ItemStack itemFromChest = itemCount.getItem(); // this item is already a copy with amount 1

			if (shopkeeper.getOffer(itemFromChest) != null) {
				continue; // already added
			}

			// add recipe:
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(itemFromChest, 0);
			recipes.add(recipe);
		}

		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(Player player, TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		int price = this.getPrice(recipe);
		if (price <= 0) return;

		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createPriceOffer(recipe.getResultItem(), price));
	}

	@Override
	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		assert this.isTradesArea(event.getRawSlot());
		int rawSlot = event.getRawSlot();
		if (this.isResultRow(rawSlot)) {
			// handle changing sell stack size:
			this.handleUpdateItemAmountOnClick(event, 1);
		} else {
			super.handleTradesClick(session, event);
		}
	}
}
