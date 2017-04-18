/*
 * Copyright (C) 2011-2016 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * This file is part of greenDAO Generator.
 *
 * greenDAO Generator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * greenDAO Generator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with greenDAO Generator.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.greenrobot.greendao.daotest.rx;

import org.greenrobot.greendao.daotest.TestEntity;
import org.greenrobot.greendao.daotest.TestEntityDao;
import org.greenrobot.greendao.rx.NullStub;
import org.greenrobot.greendao.rx.RxDao;
import org.greenrobot.greendao.test.AbstractDaoTest;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;


@SuppressWarnings("unchecked")
public class RxDaoTest extends AbstractDaoTest<TestEntityDao, TestEntity, Long> {

    private RxDao rxDao;

    public RxDaoTest() {
        super(TestEntityDao.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        rxDao = dao.rx();
    }

    public void testScheduler() {
        TestObserver<List<TestEntity>> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.loadAll());
        Thread lastSeenThread = testSubscriber.lastThread();
        assertNotSame(lastSeenThread, Thread.currentThread());
    }

    public void testNoScheduler() {
        RxDao<TestEntity, Long> rxDaoNoScheduler = dao.rxPlain();
        TestObserver<List<TestEntity>> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDaoNoScheduler.loadAll());
        Thread lastSeenThread = testSubscriber.lastThread();
        assertSame(lastSeenThread, Thread.currentThread());
    }

    public void testLoadAll() {
        insertEntity("foo");
        insertEntity("bar");

        TestObserver<List<TestEntity>> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.loadAll());
        assertEquals(1, testSubscriber.valueCount());
        List<TestEntity> entities = testSubscriber.values().get(0);

        // Order of entities is unspecified
        int foo = 0, bar = 0;
        for (TestEntity entity : entities) {
            String value = entity.getSimpleStringNotNull();
            if (value.equals("foo")) {
                foo++;
            } else if (value.equals("bar")) {
                bar++;
            } else {
                fail(value);
            }
        }
        assertEquals(1, foo);
        assertEquals(1, bar);
    }

    public void testLoad() {
        TestEntity foo = insertEntity("foo");
        TestObserver<TestEntity> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.load(foo.getId()));
        assertEquals(1, testSubscriber.valueCount());
        TestEntity foo2 = testSubscriber.values().get(0);
        assertEquals(foo.getSimpleStringNotNull(), foo2.getSimpleStringNotNull());
    }

    // todo: null is not allowed in rxjava2, then how to test such case that loads no result
    // querying? The Observable returned by rxDao.laod(42) causes NullPointerException when being subscribing.
    public void testLoad_noResult() {
//        TestObserver<TestEntity> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.load(42));
        TestObserver<TestEntity> testSubscriber = RxTestHelper.awaitTestNullObserver(rxDao.load(42));
        assertEquals(0, testSubscriber.valueCount());
        // Should we really propagate null through Rx?
//        assertNull(testSubscriber.values().get(0));
    }

    public void testRefresh() {
        TestEntity entity = insertEntity("foo");
        entity.setSimpleStringNotNull("temp");
        RxTestHelper.awaitTestSubscriber(rxDao.refresh(entity));
        assertEquals("foo", entity.getSimpleStringNotNull());
    }

    public void testInsert() {
        TestEntity foo = RxTestHelper.createEntity("foo");
        TestObserver<TestEntity> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.insert(foo));
        assertEquals(1, testSubscriber.valueCount());
        TestEntity foo2 = testSubscriber.values().get(0);
        assertSame(foo, foo2);

        List<TestEntity> all = dao.loadAll();
        assertEquals(1, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
    }

    public void testInsertInTx() {
        TestEntity foo = RxTestHelper.createEntity("foo");
        TestEntity bar = RxTestHelper.createEntity("bar");
        TestObserver<Object[]> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.insertInTx(foo, bar));
        assertEquals(1, testSubscriber.valueCount());
        Object[] array = testSubscriber.values().get(0);
        assertSame(foo, array[0]);
        assertSame(bar, array[1]);

        List<TestEntity> all = dao.loadAll();
        assertEquals(2, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
        assertEquals(bar.getSimpleStringNotNull(), all.get(1).getSimpleStringNotNull());
    }

    public void testInsertInTxList() {
        TestEntity foo = RxTestHelper.createEntity("foo");
        TestEntity bar = RxTestHelper.createEntity("bar");
        List<TestEntity> list = new ArrayList<>();
        list.add(foo);
        list.add(bar);
        TestObserver<List<TestEntity>> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.insertInTx(list));
        assertEquals(1, testSubscriber.valueCount());
        List<TestEntity> result = testSubscriber.values().get(0);
        assertSame(foo, result.get(0));
        assertSame(bar, result.get(1));

        List<TestEntity> all = dao.loadAll();
        assertEquals(2, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
        assertEquals(bar.getSimpleStringNotNull(), all.get(1).getSimpleStringNotNull());
    }

    public void testInsertOrReplace() {
        TestEntity foo = insertEntity("foo");

        foo.setSimpleStringNotNull("bar");

        assertUpdatedEntity(foo, rxDao.insertOrReplace(foo));
    }

    public void testInsertOrReplaceInTx() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");

        assertUpdatedEntities(foo, bar, rxDao.insertOrReplaceInTx(foo, bar));
    }

    public void testInsertOrReplaceInTxList() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");

        List<TestEntity> list = new ArrayList<>();
        list.add(foo);
        list.add(bar);

        assertUpdatedEntities(list, rxDao.insertOrReplaceInTx(list));
    }

    public void testSave() {
        TestEntity foo = insertEntity("foo");

        foo.setSimpleStringNotNull("bar");

        assertUpdatedEntity(foo, rxDao.save(foo));
    }

    public void testSaveInTx() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");

        assertUpdatedEntities(foo, bar, rxDao.saveInTx(foo, bar));
    }

    public void testSaveInTxList() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");

        List<TestEntity> list = new ArrayList<>();
        list.add(foo);
        list.add(bar);

        assertUpdatedEntities(list, rxDao.saveInTx(list));
    }

    public void testUpdate() {
        TestEntity foo = insertEntity("foo");
        foo.setSimpleString("foofoo");
        TestObserver testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.update(foo));
        assertEquals(1, testSubscriber.valueCount());
        assertSame(foo, testSubscriber.values().get(0));
        List<TestEntity> testEntities = dao.loadAll();
        assertEquals(1, testEntities.size());
        assertNotSame(foo, testEntities.get(0));
        assertEquals("foofoo", testEntities.get(0).getSimpleString());
    }

    public void testUpdateInTx() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");
        bar.setSimpleStringNotNull("bar2");

        assertUpdatedEntities(foo, bar, rxDao.updateInTx(foo, bar));
    }

    public void testUpdateInTxList() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        foo.setSimpleStringNotNull("foo2");
        bar.setSimpleStringNotNull("bar2");

        List<TestEntity> list = new ArrayList<>();
        list.add(foo);
        list.add(bar);

        assertUpdatedEntities(list, rxDao.updateInTx(list));
    }

    private void assertUpdatedEntity(TestEntity foo, Observable<TestEntity> observable) {
        TestObserver<TestEntity> testSubscriber = RxTestHelper.awaitTestSubscriber(observable);
        assertEquals(1, testSubscriber.valueCount());
        TestEntity bar = testSubscriber.values().get(0);
        assertSame(foo, bar);

        List<TestEntity> all = dao.loadAll();
        assertEquals(1, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
    }

    private void assertUpdatedEntities(TestEntity foo, TestEntity bar, Observable<Object[]> observable) {
        TestObserver<Object[]> testSubscriber = RxTestHelper.awaitTestSubscriber(observable);
        assertEquals(1, testSubscriber.valueCount());
        Object[] array = testSubscriber.values().get(0);
        assertSame(foo, array[0]);
        assertSame(bar, array[1]);

        List<TestEntity> all = dao.loadAll();
        assertEquals(2, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
        assertEquals(bar.getSimpleStringNotNull(), all.get(1).getSimpleStringNotNull());
    }

    private void assertUpdatedEntities(List<TestEntity> entities, Observable<List<TestEntity>> observable) {
        TestEntity foo = entities.get(0);
        TestEntity bar = entities.get(1);

        TestObserver<List<TestEntity>> testSubscriber = RxTestHelper.awaitTestSubscriber(observable);
        assertEquals(1, testSubscriber.valueCount());
        List<TestEntity> result = testSubscriber.values().get(0);
        assertSame(foo, result.get(0));
        assertSame(bar, result.get(1));

        List<TestEntity> all = dao.loadAll();
        assertEquals(2, all.size());
        assertEquals(foo.getSimpleStringNotNull(), all.get(0).getSimpleStringNotNull());
        assertEquals(bar.getSimpleStringNotNull(), all.get(1).getSimpleStringNotNull());
    }

    public void testDelete() {
        TestEntity foo = insertEntity("foo");
        assertDeleted(rxDao.delete(foo));
    }

    public void testDeleteByKey() {
        TestEntity foo = insertEntity("foo");
        assertDeleted(rxDao.deleteByKey(foo.getId()));
    }

    public void testDeleteAll() {
        insertEntity("foo");
        insertEntity("bar");
        assertDeleted(rxDao.deleteAll());
    }

    public void testDeleteInTx() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");
        assertDeleted(rxDao.deleteInTx(foo, bar));
    }

    public void testDeleteInTxList() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        List<TestEntity> list = new ArrayList<>();
        list.add(foo);
        list.add(bar);

        assertDeleted(rxDao.deleteInTx(list));
    }

    public void testDeleteByKeyInTx() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");
        assertDeleted(rxDao.deleteByKeyInTx(foo.getId(), bar.getId()));
    }

    public void testDeleteByKeyInTxList() {
        TestEntity foo = insertEntity("foo");
        TestEntity bar = insertEntity("bar");

        List<Long> list = new ArrayList<>();
        list.add(foo.getId());
        list.add(bar.getId());

        assertDeleted(rxDao.deleteByKeyInTx(list));
    }

    private void assertDeleted(Observable<Void> observable) {
        TestObserver testSubscriber = RxTestHelper.awaitTestSubscriber(observable);
        assertEquals(1, testSubscriber.valueCount());
//        assertNull(testSubscriber.values().get(0));
        assertEquals(NullStub.NULL, testSubscriber.values().get(0));
        assertEquals(0, dao.count());
    }

    public void testCount() {
        insertEntity("foo");
        TestObserver<Long> testSubscriber = RxTestHelper.awaitTestSubscriber(rxDao.count());
        assertEquals(1, testSubscriber.valueCount());
        Long count = testSubscriber.values().get(0);
        assertEquals(1L, (long) count);
    }

    protected TestEntity insertEntity(String simpleStringNotNull) {
        return RxTestHelper.insertEntity(dao, simpleStringNotNull);
    }
}
