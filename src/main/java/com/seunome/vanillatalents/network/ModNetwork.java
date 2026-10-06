package com.seunome.vanillatalents.network;

import com.seunome.vanillatalents.VanillaTalents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetwork {

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(Identifier.fromNamespaceAndPath(VanillaTalents.MODID, "main"))
            .networkProtocolVersion(2)
            .simpleChannel();

    private ModNetwork() {}

    public static void register() {
        CHANNEL.messageBuilder(C2SConvertXp.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SConvertXp::encode).decoder(C2SConvertXp::decode)
                .consumerMainThread(C2SConvertXp::handle).add();
        CHANNEL.messageBuilder(C2SBuyNode.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SBuyNode::encode).decoder(C2SBuyNode::decode)
                .consumerMainThread(C2SBuyNode::handle).add();
        CHANNEL.messageBuilder(C2SChangeClass.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(C2SChangeClass::encode).decoder(C2SChangeClass::decode)
                .consumerMainThread(C2SChangeClass::handle).add();
        CHANNEL.messageBuilder(S2CSyncPlayer.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncPlayer::encode).decoder(S2CSyncPlayer::decode)
                .consumerMainThread(S2CSyncPlayer::handle).add();
        CHANNEL.messageBuilder(S2CSyncDefinitions.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CSyncDefinitions::encode).decoder(S2CSyncDefinitions::decode)
                .consumerMainThread(S2CSyncDefinitions::handle).add();
        CHANNEL.messageBuilder(S2CProspectorHighlight.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(S2CProspectorHighlight::encode).decoder(S2CProspectorHighlight::decode)
                .consumerMainThread(S2CProspectorHighlight::handle).add();
        CHANNEL.build();
    }

    public static void sendTo(ServerPlayer player, Object message) {
        CHANNEL.send(message, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object message) {
        CHANNEL.send(message, PacketDistributor.SERVER.noArg());
    }
}
