#!/usr/bin/env python3
"""Declared raw + FG-mapped inputs and native SDK ABI, never historical run fixtures."""
import argparse
import hashlib
import json
import subprocess
import zipfile

RAW_SHA = '6132fc23a2fe1da0ca57439b35526ee9a1a8d898397ff8e61b2d08fb951cdf6a'
URN = 'net/mehvahdjukaar/supplementaries/common/block/blocks/UrnBlock'
TILE = 'net/mehvahdjukaar/supplementaries/common/block/tiles/UrnBlockTile'
RAW_CLASSES = {URN: 'b088bcc23b678abdfb479c3c33ab9e0d4d3fae2f46c81eb111cd4f4ee10653c7',
               TILE: 'a3cabed968d4e0c09e34b8e933845291595f8e91a3f0de5e0031730288a00cf5'}
MAPPED_CLASSES = {URN: '68884aeaa5e9389ca631a2f185e799b2fc215c3f3b239905934437fc376fceff',
                  TILE: '8112ff27f15ee53f2c164acdac18c035eb15f3a0bee99d763a46e441c565d435'}
SDK = {
 'net/minecraft/world/level/chunk/ImposterProtoChunk': ('b6f8c8e2392de34b7962044cabe5ab956074ec317665ae909db2c1ff0b4ab952', '74e41c29079636ebb00ebf062b981f1c5037ec434072ec92116d3798a5660c63'),
 'net/minecraft/server/level/WorldGenRegion': ('7812f15df307ddd0cdcae8ef08223a716a624de00f3e512c0f49239920d10db9', '038e9cbe8b414845da295611e70caeac6cca1ac9efdfc1f163f9fa213c1ab6cd'),
 'net/minecraft/world/level/chunk/ProtoChunk': ('3056e0fcc46f286f64d1003b5278e6b6e7c402fb032ae2a53c15edc5416be44b', 'cc8e03e7b37f15064cb79b32a8828e5a99ee1cad655ca8c15ec3037c0c8457f3'),
 'net/minecraft/world/level/storage/loot/LootContext$Builder': ('da6084d2e8b65b3781486e5a45061af9aa59601e187694c7cf86c73a8c561c57', '99ed98b1482a42995330b3cd2815c39ec937c4abd00075ca1b9c4f380a40a222'),
 'net/minecraft/world/level/storage/loot/LootParams$Builder': ('2ecd84f2bf140abfc8e225ec813e86a4909104a64e67bf8e032ab277988d69af', 'd4b797605aa007d468c20640e133c62f1818c91493588ab84b56813679088e1d'),
 'net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase': ('ec49ba5e67c5272060397ed4fccf512ab7e2cf965da8295db850321540ebaa70', '2d0e63e450caead1710a462bc66ce913083a8c22e25be23e4e1cdaddb5d73e12'),
 'net/minecraft/world/level/chunk/ChunkAccess': ('bc726dcb749faec8476b59e0153d86d3b32e61fb6c80248504a46fc7289f248a', '69fcd61049f3c382f54a35219fc665fe8cd9c17904662fa9439c805e60f2848b'),
 'net/minecraft/world/level/chunk/LevelChunk': ('2d74f57e549e8e190bfcc83771ff3b97042638ed10e8aaa0e45e8728f5d7c13e', 'cb75c2eeff49df4b7b1c5a7ce46113f9fee197ac952ef37b60032a289f2e05e9'),
 'net/minecraft/world/level/chunk/storage/ChunkSerializer': ('ba60daef731e0af40e36857b348a16472df17235356462177e1421a07f56e98d', 'a5b7110146d6a84577b4d95bf4a7e6de64920be08e991ed1278952cb05924b2e'),
}
def sha(data): return hashlib.sha256(data).hexdigest()
def require(value, msg):
    if not value: raise AssertionError(msg)
def code(path, cls):
    return subprocess.run(['javap', '-classpath', path, '-c', '-p', cls.replace('/', '.')],
                          text=True, capture_output=True, check=True, timeout=30).stdout

def verify(args):
    with open(args.raw, 'rb') as stream: require(sha(stream.read()) == RAW_SHA, 'Unreviewed raw Supplementaries archive')
    for path, classes in [(args.raw, RAW_CLASSES), (args.mapped, MAPPED_CLASSES)]:
        with zipfile.ZipFile(path) as z:
            for name, digest in classes.items(): require(sha(z.read(name + '.class')) == digest, 'Unreviewed urn class ' + name)
    with zipfile.ZipFile(args.raw) as z:
        urn = json.loads(z.read('data/supplementaries/loot_tables/blocks/urn.json'))
        pools = urn['pools']; require(len(pools) == 2, 'Urn pool population drift')
        silk = pools[0]['entries'][0]['conditions'][0]['predicate']['enchantments'][0]
        require(silk == {'enchantment':'minecraft:silk_touch','levels':{'min':1}}, 'Native Silk Touch contract drift')
        require(pools[1]['entries'][0]['conditions'][0]['properties'] == {'treasure':'true'}, 'Native treasure loot gate drift')
        feature = json.loads(z.read('data/supplementaries/worldgen/configured_feature/cave_urns.json'))
        require(feature['type'] == 'minecraft:random_patch' and feature['config']['tries'] == 9, 'Native producer drift')
        state = feature['config']['feature']['feature']['config']['to_place']['state']
        require(state == {'Name':'supplementaries:urn','Properties':{'waterlogged':'false','treasure':'true'}}, 'Native producer state drift')
    raw = code(args.raw, URN)
    factory = raw.split(' m_142194_(', 1)[1].split('\n  public ', 1)[0]
    require('ifne' in factory and 'UrnBlockTile' in factory and 'aconst_null' in factory, 'Intentional null factory drift')
    drops = raw.split(' m_49635_(', 1)[1].split('\n  public ', 1)[0]
    require(drops.index('instanceof') < drops.index('float 0.25f'), 'Native BE-sensitive drop branch drift')
    require('Float' not in drops and 'float 0.25f' in drops and 'Stream.filter' in drops and 'ifne' in drops, 'Fortune/filter/reroll drift')
    require('ModCreativeTabs.isHidden:' in raw, 'Native hidden filter drift')
    for sdk, index in [(args.sdk_mapped, 0), (args.sdk_srg, 1)]:
        with zipfile.ZipFile(sdk) as z:
            for name, pair in SDK.items(): require(sha(z.read(name+'.class')) == pair[index], 'Unreviewed native SDK ABI/lifecycle ' + name)
    # Source47.4.13 and pack47.4.22 have independently inspected byte-identical relevant classes.
    builder = code(args.sdk_mapped, 'net/minecraft/world/level/storage/loot/LootContext$Builder')
    require('LootContext."<init>":(Lnet/minecraft/world/level/storage/loot/LootParams;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/storage/loot/LootDataResolver;Lnet/minecraft/resources/ResourceLocation;)V' in builder, 'Scoped native constructor RNG-input ABI drift')
    mapped_urn = code(args.mapped, URN)
    spawn = mapped_urn.split('void spawnAfterBreak(', 1)[1].split('\n  public ', 1)[0]
    for call in ['ServerLevel.random:Lnet/minecraft/util/RandomSource;', 'ServerLevel.getRandom:()Lnet/minecraft/util/RandomSource;', 'ServerLevel.addFreshEntity:(Lnet/minecraft/world/entity/Entity;)Z']:
        require(call in spawn, 'Scoped native spawn input/forwarded observation ABI drift: '+call)
    source = code(args.sdk_mapped, 'net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase')
    production = code(args.sdk_srg, 'net/minecraft/world/level/block/state/BlockBehaviour$BlockStateBase')
    require('boolean hasBlockEntity();' in source and 'boolean m_155947_();' in production, 'State ABI mapping drift')
    proto = code(args.sdk_mapped, 'net/minecraft/world/level/chunk/ProtoChunk')
    direct = proto.split('getBlockEntity(net.minecraft.core.BlockPos)', 1)[1].split('\n  public ', 1)[0]
    require('Map.get:' in direct and 'Map.remove:' not in direct and 'loadStatic:' not in direct, 'Proto live getter ceased to be a direct map read')
    for cls in ['net/minecraft/world/level/chunk/ProtoChunk', 'net/minecraft/world/level/chunk/LevelChunk']:
        direct_map = code(args.sdk_mapped, cls).split('getBlockEntities()', 1)[1].split('\n  public ', 1)[0]
        require('Field blockEntities:Ljava/util/Map;' in direct_map and 'invoke' not in direct_map.split('areturn', 1)[0], 'Unsupported effectful live-map accessor')
    imposter = code(args.sdk_mapped, 'net/minecraft/world/level/chunk/ImposterProtoChunk')
    wrapped = imposter.split('getWrapped()', 1)[1].split('\n  public ', 1)[0]
    require('Field wrapped:Lnet/minecraft/world/level/chunk/LevelChunk;' in wrapped and 'invoke' not in wrapped.split('areturn', 1)[0], 'Unsafe imposter unwrap accessor')
    region = code(args.sdk_mapped, 'net/minecraft/server/level/WorldGenRegion')
    region_srg = code(args.sdk_srg, 'net/minecraft/server/level/WorldGenRegion')
    require(' m_7702_(net.minecraft.core.BlockPos)' in region_srg, 'Region getter SRG ABI drift')
    cached = region.split('getChunk(int, int, net.minecraft.world.level.chunk.ChunkStatus, boolean)', 1)[1].split('\n  public ', 1)[0]
    require('Field cache:Ljava/util/List;' in cached and 'List.get:' in cached and 'Chunk is not of correct status' in cached and 'getChunkSource:' not in cached, 'Native cached region/error path drift')
    region_lookup = region.split('getBlockEntity(net.minecraft.core.BlockPos)', 1)[1].split('\n  public ', 1)[0]
    require(region_lookup.index('getChunk:') < region_lookup.index('ChunkAccess.getBlockEntity:') < region_lookup.index('ChunkAccess.getBlockEntityNbt:'), 'Native region lookup order drift')
    require('Tried to access a block entity before it was created.' in region_lookup and 'BlockEntity.loadStatic:' in region_lookup, 'Native region diagnostic/real decoder drift')
    producer = region.split('boolean setBlock(net.minecraft.core.BlockPos,', 1)[1].split('\n  private ', 1)[0]
    require('String DUMMY' in producer and 'setBlockEntityNbt:' in producer, 'Native synthetic producer drift')
    chunk = code(args.sdk_mapped, 'net/minecraft/world/level/chunk/LevelChunk')
    raw_chunk = code(args.sdk_srg, 'net/minecraft/world/level/chunk/LevelChunk')
    require(' m_5685_(net.minecraft.core.BlockPos, net.minecraft.world.level.chunk.LevelChunk$EntityCreationType)' in raw_chunk, 'Lookup SRG ABI drift')
    lookup = chunk.split('getBlockEntity(net.minecraft.core.BlockPos, net.minecraft.world.level.chunk.LevelChunk$EntityCreationType)', 1)[1].split('\n  public ', 1)[0]
    require(lookup.index('Map.remove:') < lookup.index('promotePendingBlockEntity:'), 'Original pending consumption ordering drift')
    access = code(args.sdk_mapped, 'net/minecraft/world/level/chunk/ChunkAccess')
    getter = access.split('getBlockEntityNbt(net.minecraft.core.BlockPos)', 1)[1].split('\n  public ', 1)[0]
    require('Map.get:' in getter and 'Map.remove:' not in getter and 'getBlockEntity:' not in getter, 'Unsafe materializing pending accessor')
    save = chunk.split('getBlockEntityNbtForSaving(net.minecraft.core.BlockPos)', 1)[1].split('\n  public ', 1)[0]
    require('CompoundTag.copy:' in save and 'keepPacked' in save and 'Map.get:' in save, 'Native save fallback drift')
    post = chunk.split('void postProcessGeneration()', 1)[1].split('\n  private ', 1)[0]
    require('getBlockEntity:' in post and 'Map.clear:' in post, 'Native lifecycle clearing drift')
    serializer = code(args.sdk_mapped, 'net/minecraft/world/level/chunk/storage/ChunkSerializer')
    require('keepPacked' in serializer and 'setBlockEntityNbt:' in serializer and 'BlockEntity.loadStatic:' in serializer, 'Native packed load contract drift')
    print('Treasure urn raw/mapped factory+loot+worldgen and source/production SDK lookup/save/ABI contract PASS (not Native acceptance)')

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for name in ['raw','mapped','sdk-mapped','sdk-srg']: parser.add_argument('--'+name, required=True)
    verify(parser.parse_args())
