package ru.darkchronics.quake.events;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.darkchronics.quake.QuakePlugin;
import ru.darkchronics.quake.QuakeUserState;
import ru.darkchronics.quake.game.combat.ProjectileUtil;

public class CombatListener implements Listener {
    private final QuakePlugin plugin;

    public CombatListener(QuakePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.CARROT_ON_A_STICK) return;

        QuakeUserState state = this.plugin.userStates.get(player);
        state.weaponState.shoot(player);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() != EntityType.SNOWBALL) return;
        event.setCancelled(true);

//        ProjectileUtil.impactBFG(event);
        switch (ProjectileUtil.getProjectileType(event.getEntity())) {
            case "rocket":
                ProjectileUtil.impactRocket(event);
                break;
            case "plasma":
                ProjectileUtil.impactPlasma(event);
                break;
            case "bfg":
                ProjectileUtil.impactBFG(event);
                break;
        }
    }

    @EventHandler
    public void onKnockback(EntityKnockbackByEntityEvent event) {
        // Cancel knockback, because we have our own
        event.setCancelled(true);
    }

    // Play hit sound
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
//        if (event.getEntityType() != EntityType.PLAYER) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;
        if (event.getEntity() == event.getDamager()) return;

        Entity attacker = event.getDamager();
        float health = (float) (victim.getHealth() - event.getDamage());
        attacker.playSound(
                Sound.sound(Key.key("quake.feedback.hit"), Sound.Source.NEUTRAL, 1f, (float) Math.round((1f + (health / 66)) * 6) /6 )
        );
    }

    // No hunger
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    // No health regen from food
    @EventHandler
    public void onRegen(EntityRegainHealthEvent event) {
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED)
            event.setCancelled(true);
    }
}
