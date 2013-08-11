package com.github.igotyou.FactoryMod.managers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;

import com.github.igotyou.FactoryMod.FactoryModPlugin;
import com.github.igotyou.FactoryMod.Factorys.ProductionFactory;
import com.github.igotyou.FactoryMod.interfaces.Factory;
import com.github.igotyou.FactoryMod.interfaces.FactoryManager;
import com.github.igotyou.FactoryMod.properties.ProductionProperties;
import com.github.igotyou.FactoryMod.recipes.ProbabilisticEnchantment;
import com.github.igotyou.FactoryMod.utility.InteractionResponse;
import com.github.igotyou.FactoryMod.utility.InteractionResponse.InteractionResult;
import com.github.igotyou.FactoryMod.recipes.ProductionRecipe;
import com.github.igotyou.FactoryMod.utility.ItemList;
import com.github.igotyou.FactoryMod.utility.NamedItemStack;
import java.util.Iterator;
import org.bukkit.configuration.ConfigurationSection;

public class ProductionManager implements FactoryManager
{
	public  Map<String, ProductionProperties> productionProperties=new HashMap<String, ProductionProperties>();
	public  Map<String,ProductionRecipe> productionRecipes=new HashMap<String,ProductionRecipe>();
	private FactoryModPlugin plugin;
	private List<ProductionFactory> productionFactories=new ArrayList<ProductionFactory>();;
	private long repairTime;
	
	public ProductionManager(FactoryModPlugin plugin)
	{
		this.plugin = plugin;
		initConfig(plugin.getConfig().getConfigurationSection("production"));
		//Set maintenance clock to 0
		updateFactorys();
	}
	
	public void save(File file) throws IOException 
	{
		//Takes difference between last repair update and current one and scales repair accordingly
		updateRepair(System.currentTimeMillis()-repairTime);
		repairTime=System.currentTimeMillis();
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
		for (ProductionFactory production : productionFactories)
		{
			//order: subFactoryType world recipe1,recipe2 central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			
			Location centerlocation = production.getCenterLocation();
			Location inventoryLoctation = production.getInventoryLocation();
			Location powerLocation = production.getPowerSourceLocation();
			
			
			
			bufferedWriter.append(production.getSubFactoryType());
			bufferedWriter.append(" ");
			
			List<ProductionRecipe> recipes=production.getRecipes();
			for (int i = 0; i < recipes.size(); i++)
			{
				bufferedWriter.append(String.valueOf(recipes.get(i).getTitle()));
				bufferedWriter.append(",");
			}
			bufferedWriter.append(" ");
			
			bufferedWriter.append(centerlocation.getWorld().getName());
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(centerlocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(inventoryLoctation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Integer.toString(powerLocation.getBlockX()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockY()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(powerLocation.getBlockZ()));
			bufferedWriter.append(" ");
			
			bufferedWriter.append(Boolean.toString(production.getActive()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getProductionTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getEnergyTimer()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Integer.toString(production.getCurrentRecipeNumber()));
			bufferedWriter.append(" ");
			bufferedWriter.append(Double.toString(production.getCurrentRepair()));
			bufferedWriter.append(" ");
			bufferedWriter.append(String.valueOf(production.getTimeDisrepair()));
			bufferedWriter.append("\n");
		}
		bufferedWriter.flush();
		fileOutputStream.close();
	}

	public void load(File file) throws IOException 
	{
		repairTime=System.currentTimeMillis();
		FileInputStream fileInputStream = new FileInputStream(file);
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));
		String line;
		while ((line = bufferedReader.readLine()) != null)
		{
			String parts[] = line.split(" ");

			//order: subFactoryType world recipe1,recipe2 central_x central_y central_z inventory_x inventory_y inventory_z power_x power_y power_z active productionTimer energyTimer current_Recipe_number 
			String subFactoryType = parts[0];
			String recipeNames[] = parts[1].split(",");

			Location centerLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
			Location inventoryLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[6]), Integer.parseInt(parts[7]), Integer.parseInt(parts[8]));
			Location powerLocation = new Location(plugin.getServer().getWorld(parts[2]), Integer.parseInt(parts[9]), Integer.parseInt(parts[10]), Integer.parseInt(parts[11]));
			boolean active = Boolean.parseBoolean(parts[12]);
			int productionTimer = Integer.parseInt(parts[13]);
			int energyTimer = Integer.parseInt(parts[14]);
			int currentRecipeNumber = Integer.parseInt(parts[15]);
			double currentRepair = Double.parseDouble(parts[16]);
			long timeDisrepair  =  Long.parseLong(parts[17]);
			if(productionProperties.containsKey(subFactoryType))
			{
				Set<ProductionRecipe> recipes=new HashSet<ProductionRecipe>();
				
				// TODO: Give default recipes for subfactory type
				ProductionProperties properties = productionProperties.get(subFactoryType);
				recipes.addAll(properties.getRecipes());
				
				for(String name:recipeNames)
				{
					if(productionRecipes.containsKey(name))
					{
						recipes.add(productionRecipes.get(name));
					}
				}

				ProductionFactory production = new ProductionFactory(centerLocation, inventoryLocation, powerLocation, subFactoryType, active, productionTimer, energyTimer, new ArrayList<ProductionRecipe>(recipes), currentRecipeNumber, currentRepair,timeDisrepair);
				addFactory(production);
			}
		}
		fileInputStream.close();
	}
	
	/*
	 * Imports settings, recipes and factories from config
	 */
	public void initConfig(ConfigurationSection productionConfiguration)
	{
		//Import recipes from config.yml
		ConfigurationSection recipeConfiguration=productionConfiguration.getConfigurationSection("recipes");
		//Temporary Storage array to store where recipes should point to each other
		HashMap<ProductionRecipe,ArrayList> outputRecipes=new HashMap<ProductionRecipe,ArrayList>();
		Iterator<String> recipeTitles=recipeConfiguration.getKeys(false).iterator();
		while (recipeTitles.hasNext())
		{
			//Section header in recipe file, also serves as unique identifier for the recipe
			//All spaces are replaced with udnerscores so they don't disrupt saving format
			//There should be a check for uniqueness of this identifier...
			String title=recipeTitles.next();
			ConfigurationSection configSection=recipeConfiguration.getConfigurationSection(title);
			title=title.replaceAll(" ","_");
			//Display name of the recipe, Deafult of "Default Name"
			String recipeName = configSection.getString("name","Default Name");
			//Production time of the recipe, default of 1
			int productionTime=configSection.getInt("production_time",2);
			//Inputs of the recipe, empty of there are no inputs
			ItemList<NamedItemStack> inputs = ItemList.fromConfig(configSection.getConfigurationSection("inputs"));
			//Inputs of the recipe, empty of there are no inputs
			ItemList<NamedItemStack> upgrades = ItemList.fromConfig(configSection.getConfigurationSection("upgrades"));
			//Outputs of the recipe, empty of there are no inputs
			ItemList<NamedItemStack> outputs = ItemList.fromConfig(configSection.getConfigurationSection("outputs"));
			//Enchantments of the recipe, empty of there are no inputs
			List<ProbabilisticEnchantment> enchantments=ProbabilisticEnchantment.listFromConfig(configSection.getConfigurationSection("enchantments"));
			//Whether this recipe can only be used once
			boolean useOnce = configSection.getBoolean("use_once");
			ProductionRecipe recipe = new ProductionRecipe(title,recipeName,productionTime,inputs,upgrades,outputs,enchantments,useOnce,new ItemList<NamedItemStack>());
			productionRecipes.put(title,recipe);
			//Store the titles of the recipes that this should point to
			ArrayList <String> currentOutputRecipes=new ArrayList<String>();
			currentOutputRecipes.addAll(configSection.getStringList("output_recipes"));
			outputRecipes.put(recipe,currentOutputRecipes);
		}
		//Once ProductionRecipe objects have been created correctly insert different pointers
		Iterator<ProductionRecipe> recipeIterator=outputRecipes.keySet().iterator();
		while (recipeIterator.hasNext())
		{
			ProductionRecipe recipe=recipeIterator.next();
			Iterator<String> outputIterator=outputRecipes.get(recipe).iterator();
			while(outputIterator.hasNext())
			{
				recipe.addOutputRecipe(productionRecipes.get(outputIterator.next()));
			}
		}
		
		//Import factories
		ConfigurationSection factoryConfiguration=productionConfiguration.getConfigurationSection("factories");
		Iterator<String> factoryTitles=factoryConfiguration.getKeys(false).iterator();
		while(factoryTitles.hasNext())
		{
			String title=factoryTitles.next();
			ProductionProperties newProductionProperties = ProductionProperties.fromConfig(title, factoryConfiguration.getConfigurationSection(title));
			productionProperties.put(title, newProductionProperties);
		}	
	}
	
	public void updateFactorys() 
	{
		plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
		{
			@Override
			public void run()
			{
				for (ProductionFactory production: productionFactories)
				{
					production.update();
				}
			}
		}, 0L, FactoryModPlugin.UPDATE_CYCLE);
	}

	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			Map<String, ProductionProperties> properties = productionProperties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				ItemList<NamedItemStack> inputs = entry.getValue().getInputs();
				if(inputs.exactlyIn(chestInventory))
				{
					subFactoryType = entry.getKey();
				}
			}
			if (subFactoryType != null)
			{
				ProductionFactory production = new ProductionFactory(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (properties.get(subFactoryType).getInputs().allIn(production.getInventory()))
				{
					addFactory(production);
					properties.get(subFactoryType).getInputs().removeFrom(production.getInventory());
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + production.getProductionFactoryProperties().getName());
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Incorrect materials in chest! Stacks must match perfectly.");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a factory there!");
	}
	
	public InteractionResponse createFactory(Location factoryLocation, Location inventoryLocation, Location powerSourceLocation, int productionTimer, int energyTimer) 
	{
		if (!factoryExistsAt(factoryLocation))
		{
			Map<String, ProductionProperties> properties = productionProperties;
			Block inventoryBlock = inventoryLocation.getBlock();
			Chest chest = (Chest) inventoryBlock.getState();
			Inventory chestInventory = chest.getInventory();
			String subFactoryType = null;
			boolean hasMaterials = true;
			for (Map.Entry<String, ProductionProperties> entry : properties.entrySet())
			{
				ItemList<NamedItemStack> inputs = entry.getValue().getInputs();
				if(!inputs.allIn(chestInventory))
				{
					hasMaterials = false;
				}
				if (hasMaterials == true)
				{
					subFactoryType = entry.getKey();
				}
			}
			if (hasMaterials && subFactoryType != null)
			{
				ProductionFactory production = new ProductionFactory(factoryLocation, inventoryLocation, powerSourceLocation,subFactoryType);
				if (properties.get(subFactoryType).getInputs().removeFrom(production.getInventory()))
				{
					addFactory(production);
					return new InteractionResponse(InteractionResult.SUCCESS, "Successfully created " + subFactoryType + " production factory");
				}
			}
			return new InteractionResponse(InteractionResult.FAILURE, "Not enough materials in chest!");
		}
		return new InteractionResponse(InteractionResult.FAILURE, "There is already a factory there!");
	}

	public InteractionResponse addFactory(Factory factory) 
	{
		ProductionFactory production = (ProductionFactory) factory;
		if (production.getCenterLocation().getBlock().getType().equals(Material.WORKBENCH) && (!factoryExistsAt(production.getCenterLocation()))
				|| !factoryExistsAt(production.getInventoryLocation()) || !factoryExistsAt(production.getPowerSourceLocation()))
		{
			productionFactories.add(production);
			return new InteractionResponse(InteractionResult.SUCCESS, "");
		}
		else
		{
			return new InteractionResponse(InteractionResult.FAILURE, "");
		}
	}

	public ProductionFactory getFactory(Location factoryLocation) 
	{
		for (ProductionFactory production : productionFactories)
		{
			if (production.getCenterLocation().equals(factoryLocation) || production.getInventoryLocation().equals(factoryLocation)
					|| production.getPowerSourceLocation().equals(factoryLocation))
				return production;
		}
		return null;
	}
	
	public boolean factoryExistsAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = true;
		}
		return returnValue;
	}
	
	public boolean factoryWholeAt(Location factoryLocation) 
	{
		boolean returnValue = false;
		if (getFactory(factoryLocation) != null)
		{
			returnValue = getFactory(factoryLocation).isWhole();
		}
		return returnValue;
	}

	public void removeFactory(Factory factory) 
	{
		productionFactories.remove((ProductionFactory)factory);
	}
	
	public void updateRepair(long time)
	{
		for (ProductionFactory production: productionFactories)
		{
			production.updateRepair(time/((double)FactoryModPlugin.REPAIR_PERIOD));
		}
		long currentTime=System.currentTimeMillis();
		Iterator<ProductionFactory> itr=productionFactories.iterator();
		while(itr.hasNext())
		{
			ProductionFactory producer=itr.next();
			if(currentTime>(producer.getTimeDisrepair()+FactoryModPlugin.DISREPAIR_PERIOD))
			{
				itr.remove();
			}
		}
	}
	
	public String getSavesFileName() 
	{
		return FactoryModPlugin.PRODUCTION_SAVES_FILE;
	}
	
	/*
	 * Returns of the ProductionProperites for a particular factory
	 */
	public ProductionProperties getProperties(String title)
	{
		return productionProperties.get(title);
	}
}
