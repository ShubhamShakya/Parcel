package com.parcel.data

import com.parcel.domain.*
import org.koin.dsl.module

val shoppingDataModule = module {
    single<CompanionRepository> { RoomCompanionRepository(get()) }
    single<AuthRepository> { RoomAuthRepository(get()) }
    single<DeliveryRepository> { RoomDeliveryRepository(get()) }
    single<CommerceRepository> { RoomCommerceRepository(get()) }
    single<CatalogRepository> { MockCatalogRepository() }
    single { get<ParcelDatabase>().shoppingDao() }
    single { RoomShoppingRepository(get()) }
    single<SavedItemsRepository> { get<RoomShoppingRepository>() }
    single<AppearanceRepository> { get<RoomShoppingRepository>() }
    single<PreferencesRepository> { get<RoomShoppingRepository>() }
}
