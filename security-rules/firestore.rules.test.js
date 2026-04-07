/**
 * ShowMate — Tests de Firestore Security Rules
 *
 * Cómo ejecutar:
 *   1. Instalar Firebase CLI: npm install -g firebase-tools
 *   2. Instalar dependencias: cd security-rules && npm install
 *   3. En una terminal: firebase emulators:start --only firestore --project showmate-test
 *   4. En otra terminal: npm test
 *
 * O con un solo comando (espera a que el emulador esté listo):
 *   firebase emulators:exec --only firestore "cd security-rules && npm test" --project showmate-test
 *
 * Variables de entorno:
 *   FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 (se setea automáticamente por el emulador)
 */

const { initializeTestEnvironment, assertFails, assertSucceeds } =
  require('@firebase/rules-unit-testing');

const {
  doc, getDoc, setDoc, addDoc, updateDoc, deleteDoc, collection,
} = require('firebase/firestore');

const fs = require('fs');
const path = require('path');

// ── Setup / Teardown ──────────────────────────────────────────────────────────

let testEnv;

beforeAll(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: 'showmate-test',
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, '../firestore.rules'), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

afterAll(async () => {
  await testEnv.cleanup();
});

// Limpiar Firestore entre tests para evitar interferencias
afterEach(async () => {
  await testEnv.clearFirestore();
});

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Contexto autenticado con uid y email opcionales */
const auth = (uid, email = `${uid}@test.com`) =>
  testEnv.authenticatedContext(uid, { email }).firestore();

/** Contexto no autenticado */
const unauth = () => testEnv.unauthenticatedContext().firestore();

/** Escribe datos en Firestore saltándose las reglas (setup de tests) */
async function seed(path, data) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await setDoc(doc(ctx.firestore(), path), data);
  });
}

/** Timestamp "reciente" (ahora) para pasar la validación isRecentMs */
const now = () => Date.now();

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: users/{uid}
// ─────────────────────────────────────────────────────────────────────────────

describe('users/{uid}', () => {

  beforeEach(async () => {
    await seed('users/alice', {
      userId: 'alice', email: 'alice@test.com', username: 'Alice',
      xp: 100, completedGroupMatches: 2, genreScores: { '18': 15.0 }
    });
    await seed('users/bob', {
      userId: 'bob', email: 'bob@test.com', username: 'Bob',
      xp: 50, completedGroupMatches: 0
    });
  });

  // ── Lectura ───────────────────────────────────────────────────────────────

  it('✓ owner puede leer su propio perfil', async () => {
    await assertSucceeds(getDoc(doc(auth('alice'), 'users/alice')));
  });

  it('✓ usuario autenticado puede leer perfil ajeno (features sociales)', async () => {
    await assertSucceeds(getDoc(doc(auth('bob'), 'users/alice')));
  });

  it('✗ usuario NO autenticado no puede leer perfiles', async () => {
    await assertFails(getDoc(doc(unauth(), 'users/alice')));
  });

  // ── Creación ──────────────────────────────────────────────────────────────

  it('✓ usuario crea su propio perfil con campos válidos', async () => {
    const db = auth('charlie', 'charlie@test.com');
    await assertSucceeds(setDoc(doc(db, 'users/charlie'), {
      userId: 'charlie', email: 'charlie@test.com', username: 'Charlie',
      xp: 0, completedGroupMatches: 0
    }));
  });

  it('✗ usuario no puede crear perfil con userId distinto', async () => {
    const db = auth('charlie');
    await assertFails(setDoc(doc(db, 'users/charlie'), {
      userId: 'hacked', email: 'charlie@test.com', username: 'Charlie',
      xp: 0, completedGroupMatches: 0
    }));
  });

  it('✗ usuario no puede crear perfil con xp > 0 inicial', async () => {
    const db = auth('charlie');
    await assertFails(setDoc(doc(db, 'users/charlie'), {
      userId: 'charlie', email: 'charlie@test.com', username: 'Charlie',
      xp: 9999, completedGroupMatches: 0
    }));
  });

  it('✗ usuario no puede crear el perfil de otro', async () => {
    const db = auth('bob');
    await assertFails(setDoc(doc(db, 'users/alice'), {
      userId: 'alice', email: 'alice@test.com', username: 'Hacked',
      xp: 0, completedGroupMatches: 0
    }));
  });

  it('✗ username demasiado largo (> 50 chars) bloqueado', async () => {
    const db = auth('charlie');
    await assertFails(setDoc(doc(db, 'users/charlie'), {
      userId: 'charlie', email: 'charlie@test.com',
      username: 'x'.repeat(51),
      xp: 0, completedGroupMatches: 0
    }));
  });

  // ── Actualización ─────────────────────────────────────────────────────────

  it('✓ owner puede actualizar su username', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(updateDoc(doc(db, 'users/alice'), { username: 'Alice2' }));
  });

  it('✗ owner no puede cambiar su email (inmutable)', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(updateDoc(doc(db, 'users/alice'), { email: 'nuevo@test.com' }));
  });

  it('✗ owner no puede reducir su xp', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(updateDoc(doc(db, 'users/alice'), { xp: 0 }));
  });

  it('✗ otro usuario no puede actualizar el perfil ajeno', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(updateDoc(doc(db, 'users/alice'), { username: 'Hacked' }));
  });

  // ── Borrado ───────────────────────────────────────────────────────────────

  it('✓ owner puede borrar su perfil', async () => {
    await assertSucceeds(deleteDoc(doc(auth('alice', 'alice@test.com'), 'users/alice')));
  });

  it('✗ otro usuario no puede borrar perfil ajeno', async () => {
    await assertFails(deleteDoc(doc(auth('bob'), 'users/alice')));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: users/{uid}/activity
// ─────────────────────────────────────────────────────────────────────────────

describe('users/{uid}/activity/{eventId}', () => {

  const validActivity = {
    userId: 'alice', username: 'Alice',
    type: 'liked', mediaId: 123,
    mediaTitle: 'Breaking Bad', mediaPoster: '/path.jpg',
    score: 0.9, timestamp: now()
  };

  it('✓ owner crea su propia actividad válida', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(
      addDoc(collection(db, 'users/alice/activity'), validActivity)
    );
  });

  it('✓ usuario autenticado lee actividad ajena (para feed)', async () => {
    await seed('users/alice/activity/ev1', validActivity);
    await assertSucceeds(getDoc(doc(auth('bob'), 'users/alice/activity/ev1')));
  });

  it('✗ usuario no autenticado no puede leer actividad', async () => {
    await seed('users/alice/activity/ev1', validActivity);
    await assertFails(getDoc(doc(unauth(), 'users/alice/activity/ev1')));
  });

  it('✗ owner no puede crear actividad con tipo inválido', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(
      addDoc(collection(db, 'users/alice/activity'), {
        ...validActivity, type: 'hacked_type'
      })
    );
  });

  it('✗ owner no puede crear actividad con userId ajeno', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(
      addDoc(collection(db, 'users/alice/activity'), {
        ...validActivity, userId: 'bob'
      })
    );
  });

  it('✗ timestamp demasiado antiguo rechazado', async () => {
    const db = auth('alice', 'alice@test.com');
    const fiveMinutesAgo = Date.now() - 5 * 60 * 1000;
    await assertFails(
      addDoc(collection(db, 'users/alice/activity'), {
        ...validActivity, timestamp: fiveMinutesAgo
      })
    );
  });

  it('✗ otro usuario no puede escribir en actividad ajena', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(
      addDoc(collection(db, 'users/alice/activity'), {
        ...validActivity, userId: 'alice'
      })
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: users/{uid}/notifications
// ─────────────────────────────────────────────────────────────────────────────

describe('users/{uid}/notifications/{notifId}', () => {

  const validNotif = {
    type: 'friend_request',
    fromUid: 'bob', fromUsername: 'Bob',
    message: 'Bob quiere ser tu amigo',
    read: false, createdAt: now()
  };

  beforeEach(async () => {
    await seed('users/alice/notifications/n1', validNotif);
  });

  it('✓ owner lee sus propias notificaciones', async () => {
    await assertSucceeds(getDoc(doc(auth('alice', 'alice@test.com'), 'users/alice/notifications/n1')));
  });

  it('✓ otro usuario autenticado puede CREAR notificación en cuenta ajena', async () => {
    // Necesario para el sistema de solicitudes de amistad
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(
      addDoc(collection(db, 'users/alice/notifications'), validNotif)
    );
  });

  it('✗ otro usuario NO puede LEER notificaciones ajenas', async () => {
    await assertFails(getDoc(doc(auth('bob'), 'users/alice/notifications/n1')));
  });

  it('✗ notificación con tipo inválido rechazada', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(
      addDoc(collection(db, 'users/alice/notifications'), {
        ...validNotif, type: 'spam_message'
      })
    );
  });

  it('✗ notificación con read=true al crear rechazada', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(
      addDoc(collection(db, 'users/alice/notifications'), {
        ...validNotif, read: true
      })
    );
  });

  it('✓ owner puede marcar notificación como leída', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(
      updateDoc(doc(db, 'users/alice/notifications/n1'), { read: true })
    );
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: friendRequests/{docId}
// ─────────────────────────────────────────────────────────────────────────────

describe('friendRequests/{docId}', () => {

  const pendingRequest = {
    id: 'req1', fromUid: 'alice', toUid: 'bob',
    fromUsername: 'Alice', toUsername: 'Bob',
    status: 'pending', createdAt: now()
  };

  beforeEach(async () => {
    await seed('friendRequests/req1', pendingRequest);
  });

  // ── Lectura ───────────────────────────────────────────────────────────────

  it('✓ emisor puede leer su solicitud enviada', async () => {
    await assertSucceeds(getDoc(doc(auth('alice', 'alice@test.com'), 'friendRequests/req1')));
  });

  it('✓ receptor puede leer la solicitud recibida', async () => {
    await assertSucceeds(getDoc(doc(auth('bob', 'bob@test.com'), 'friendRequests/req1')));
  });

  it('✗ tercero no puede leer la solicitud', async () => {
    await assertFails(getDoc(doc(auth('charlie'), 'friendRequests/req1')));
  });

  it('✗ no autenticado no puede leer solicitudes', async () => {
    await assertFails(getDoc(doc(unauth(), 'friendRequests/req1')));
  });

  // ── Creación ──────────────────────────────────────────────────────────────

  it('✓ usuario autenticado crea solicitud válida con su propio uid', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(setDoc(doc(db, 'friendRequests/req2'), {
      id: 'req2', fromUid: 'alice', toUid: 'charlie',
      fromUsername: 'Alice', toUsername: 'Charlie',
      status: 'pending', createdAt: now()
    }));
  });

  it('✗ no puede crear solicitud con fromUid ajeno (suplantación)', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(doc(db, 'friendRequests/req2'), {
      id: 'req2', fromUid: 'charlie', toUid: 'bob',  // alice finge ser charlie
      fromUsername: 'Charlie', toUsername: 'Bob',
      status: 'pending', createdAt: now()
    }));
  });

  it('✗ no puede enviarse solicitud a sí mismo', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(doc(db, 'friendRequests/self'), {
      id: 'self', fromUid: 'alice', toUid: 'alice',
      fromUsername: 'Alice', toUsername: 'Alice',
      status: 'pending', createdAt: now()
    }));
  });

  it('✗ solicitud con status inicial != "pending" rechazada', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(doc(db, 'friendRequests/req3'), {
      id: 'req3', fromUid: 'alice', toUid: 'charlie',
      fromUsername: 'Alice', toUsername: 'Charlie',
      status: 'accepted', createdAt: now()  // intento de bypass
    }));
  });

  // ── Actualización (aceptar) ───────────────────────────────────────────────

  it('✓ receptor puede aceptar la solicitud (pending → accepted)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(
      updateDoc(doc(db, 'friendRequests/req1'), { status: 'accepted' })
    );
  });

  it('✗ emisor NO puede aceptar su propia solicitud', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(
      updateDoc(doc(db, 'friendRequests/req1'), { status: 'accepted' })
    );
  });

  it('✗ receptor no puede cambiar otros campos (fromUid, toUid...)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(
      updateDoc(doc(db, 'friendRequests/req1'), {
        status: 'accepted', fromUid: 'hacked'  // intenta cambiar fromUid también
      })
    );
  });

  // ── Borrado ───────────────────────────────────────────────────────────────

  it('✓ emisor puede cancelar la solicitud', async () => {
    await assertSucceeds(deleteDoc(doc(auth('alice', 'alice@test.com'), 'friendRequests/req1')));
  });

  it('✓ receptor puede rechazar borrando la solicitud', async () => {
    await assertSucceeds(deleteDoc(doc(auth('bob', 'bob@test.com'), 'friendRequests/req1')));
  });

  it('✗ tercero no puede borrar la solicitud', async () => {
    await assertFails(deleteDoc(doc(auth('charlie'), 'friendRequests/req1')));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: reviews/{reviewId}
// ─────────────────────────────────────────────────────────────────────────────

describe('reviews/{reviewId}', () => {

  const validReview = {
    id: 'rev1', mediaId: 1396, seasonNumber: 0,
    userId: 'alice', userEmail: 'alice@test.com', username: 'Alice',
    rating: 5, text: 'Obra maestra del drama', hasSpoiler: false,
    likedByIds: [], likeCount: 0, reportCount: 0,
    createdAt: now(), updatedAt: now()
  };

  beforeEach(async () => {
    await seed('reviews/rev1', validReview);
  });

  // ── Lectura ───────────────────────────────────────────────────────────────

  it('✓ usuario autenticado puede leer reseñas', async () => {
    await assertSucceeds(getDoc(doc(auth('bob'), 'reviews/rev1')));
  });

  it('✗ usuario no autenticado no puede leer reseñas', async () => {
    await assertFails(getDoc(doc(unauth(), 'reviews/rev1')));
  });

  // ── Creación ──────────────────────────────────────────────────────────────

  it('✓ usuario crea reseña válida con su propio userId', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(setDoc(doc(db, 'reviews/rev2'), {
      id: 'rev2', mediaId: 1396, seasonNumber: 1,
      userId: 'bob', userEmail: 'bob@test.com', username: 'Bob',
      rating: 4, text: 'Gran temporada', hasSpoiler: true,
      likedByIds: [], likeCount: 0, reportCount: 0,
      createdAt: now(), updatedAt: now()
    }));
  });

  it('✗ no puede crear reseña con userId ajeno (suplantación)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(setDoc(doc(db, 'reviews/rev_fake'), {
      ...validReview, id: 'rev_fake', userId: 'alice'  // bob finge ser alice
    }));
  });

  it('✗ rating > 5 rechazado', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(setDoc(doc(db, 'reviews/rev_bad'), {
      id: 'rev_bad', mediaId: 100, seasonNumber: 0,
      userId: 'bob', userEmail: 'bob@test.com', username: 'Bob',
      rating: 6, text: 'Test', hasSpoiler: false,  // rating inválido
      likedByIds: [], likeCount: 0, reportCount: 0,
      createdAt: now(), updatedAt: now()
    }));
  });

  it('✗ texto > 2000 caracteres rechazado', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(setDoc(doc(db, 'reviews/rev_long'), {
      id: 'rev_long', mediaId: 100, seasonNumber: 0,
      userId: 'bob', userEmail: 'bob@test.com', username: 'Bob',
      rating: 3, text: 'x'.repeat(2001), hasSpoiler: false,  // texto demasiado largo
      likedByIds: [], likeCount: 0, reportCount: 0,
      createdAt: now(), updatedAt: now()
    }));
  });

  it('✗ nueva reseña con likeCount > 0 rechazada', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(setDoc(doc(db, 'reviews/rev_likes'), {
      id: 'rev_likes', mediaId: 100, seasonNumber: 0,
      userId: 'bob', userEmail: 'bob@test.com', username: 'Bob',
      rating: 3, text: 'Test', hasSpoiler: false,
      likedByIds: ['uid1', 'uid2'], likeCount: 2, reportCount: 0,  // likes inflados
      createdAt: now(), updatedAt: now()
    }));
  });

  // ── Actualización: edición del autor ─────────────────────────────────────

  it('✓ autor puede editar texto y rating de su reseña', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(updateDoc(doc(db, 'reviews/rev1'), {
      rating: 4, text: 'Actualizado', hasSpoiler: false,
      seasonNumber: 0, updatedAt: now()
    }));
  });

  it('✗ no-autor no puede editar el texto de la reseña', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(updateDoc(doc(db, 'reviews/rev1'), {
      rating: 1, text: 'Edición maliciosa', hasSpoiler: false,
      seasonNumber: 0, updatedAt: now()
    }));
  });

  it('✗ autor no puede cambiar mediaId (campo inmutable)', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(updateDoc(doc(db, 'reviews/rev1'), {
      mediaId: 9999, rating: 4, text: 'Test', hasSpoiler: false,
      seasonNumber: 0, updatedAt: now()
    }));
  });

  // ── Actualización: like ───────────────────────────────────────────────────

  it('✓ cualquier usuario puede dar like (likedByIds + likeCount)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(updateDoc(doc(db, 'reviews/rev1'), {
      likedByIds: ['bob'], likeCount: 1
    }));
  });

  it('✗ likeCount no coincide con likedByIds.size → rechazado', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(updateDoc(doc(db, 'reviews/rev1'), {
      likedByIds: ['bob', 'charlie'], likeCount: 5  // no coincide
    }));
  });

  // ── Actualización: report ─────────────────────────────────────────────────

  it('✓ cualquier usuario puede reportar una reseña (+1 reportCount)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(updateDoc(doc(db, 'reviews/rev1'), {
      reportCount: 1  // prev era 0, ahora 1
    }));
  });

  it('✗ no se puede incrementar reportCount en más de 1', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(updateDoc(doc(db, 'reviews/rev1'), {
      reportCount: 5  // intento de inflar reportes
    }));
  });

  // ── Borrado ───────────────────────────────────────────────────────────────

  it('✓ autor puede borrar su propia reseña', async () => {
    await assertSucceeds(deleteDoc(doc(auth('alice', 'alice@test.com'), 'reviews/rev1')));
  });

  it('✗ otro usuario no puede borrar la reseña', async () => {
    await assertFails(deleteDoc(doc(auth('bob'), 'reviews/rev1')));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: groupSessions/{sessionId}
// ─────────────────────────────────────────────────────────────────────────────

describe('groupSessions/{sessionId}', () => {

  const validSession = {
    id: 'session1',
    hostEmail: 'alice@test.com',
    memberEmails: ['alice@test.com', 'bob@test.com'],
    candidateIds: [],
    status: 'lobby',
    matchedMediaId: 0,
    createdAt: now()
  };

  beforeEach(async () => {
    await seed('groupSessions/session1', validSession);
    await seed('groupSessions/session1/votes/alice@test.com', {
      email: 'alice@test.com', yes: [], no: [], maybe: [], ready: false
    });
  });

  // ── Lectura ───────────────────────────────────────────────────────────────

  it('✓ miembro puede leer la sesión', async () => {
    await assertSucceeds(getDoc(doc(auth('alice', 'alice@test.com'), 'groupSessions/session1')));
  });

  it('✓ otro miembro también puede leer la sesión', async () => {
    await assertSucceeds(getDoc(doc(auth('bob', 'bob@test.com'), 'groupSessions/session1')));
  });

  it('✗ usuario no miembro no puede leer la sesión', async () => {
    await assertFails(getDoc(doc(auth('charlie', 'charlie@test.com'), 'groupSessions/session1')));
  });

  it('✗ no autenticado no puede leer sesiones', async () => {
    await assertFails(getDoc(doc(unauth(), 'groupSessions/session1')));
  });

  // ── Creación ──────────────────────────────────────────────────────────────

  it('✓ host crea sesión válida con su email', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(setDoc(doc(db, 'groupSessions/session2'), {
      id: 'session2',
      hostEmail: 'alice@test.com',
      memberEmails: ['alice@test.com', 'charlie@test.com'],
      candidateIds: [],
      status: 'lobby',
      matchedMediaId: 0,
      createdAt: now()
    }));
  });

  it('✗ no puede crear sesión con hostEmail ajeno', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertFails(setDoc(doc(db, 'groupSessions/session3'), {
      id: 'session3',
      hostEmail: 'alice@test.com',  // bob finge ser alice
      memberEmails: ['alice@test.com', 'bob@test.com'],
      candidateIds: [], status: 'lobby', matchedMediaId: 0,
      createdAt: now()
    }));
  });

  it('✗ sesión con status != "lobby" al crear rechazada', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(doc(db, 'groupSessions/session4'), {
      id: 'session4',
      hostEmail: 'alice@test.com',
      memberEmails: ['alice@test.com', 'bob@test.com'],
      candidateIds: [], status: 'finished', matchedMediaId: 0,  // bypass
      createdAt: now()
    }));
  });

  it('✗ sesión con un solo miembro rechazada', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(doc(db, 'groupSessions/session5'), {
      id: 'session5',
      hostEmail: 'alice@test.com',
      memberEmails: ['alice@test.com'],  // mínimo 2 miembros
      candidateIds: [], status: 'lobby', matchedMediaId: 0,
      createdAt: now()
    }));
  });

  // ── Actualización ─────────────────────────────────────────────────────────

  it('✓ miembro puede actualizar la sesión (añadir candidatos)', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(
      updateDoc(doc(db, 'groupSessions/session1'), { candidateIds: [1396, 1399] })
    );
  });

  it('✗ no miembro no puede actualizar la sesión', async () => {
    const db = auth('charlie', 'charlie@test.com');
    await assertFails(
      updateDoc(doc(db, 'groupSessions/session1'), { candidateIds: [9999] })
    );
  });

  // ── Votos ─────────────────────────────────────────────────────────────────

  it('✓ miembro puede escribir su propio documento de votos', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertSucceeds(setDoc(
      doc(db, 'groupSessions/session1/votes/alice@test.com'),
      { email: 'alice@test.com', yes: [1396], no: [], maybe: [], ready: false }
    ));
  });

  it('✗ miembro no puede escribir el voto de otro', async () => {
    const db = auth('alice', 'alice@test.com');
    await assertFails(setDoc(
      doc(db, 'groupSessions/session1/votes/bob@test.com'),  // alice escribe el voto de bob
      { email: 'bob@test.com', yes: [], no: [1396], maybe: [], ready: false }
    ));
  });

  it('✓ miembro puede leer votos de otros miembros', async () => {
    const db = auth('bob', 'bob@test.com');
    await assertSucceeds(getDoc(
      doc(db, 'groupSessions/session1/votes/alice@test.com')
    ));
  });

  it('✗ no miembro no puede leer votos', async () => {
    const db = auth('charlie', 'charlie@test.com');
    await assertFails(getDoc(
      doc(db, 'groupSessions/session1/votes/alice@test.com')
    ));
  });
});

// ─────────────────────────────────────────────────────────────────────────────
// SUITE: Casos de ataque específicos
// ─────────────────────────────────────────────────────────────────────────────

describe('Ataques y edge cases', () => {

  it('✗ usuario no autenticado no puede escribir en ninguna colección', async () => {
    const db = unauth();
    await assertFails(setDoc(doc(db, 'users/hacker'), { userId: 'hacker', email: 'h@h.com' }));
    await assertFails(setDoc(doc(db, 'friendRequests/r1'), { fromUid: 'x', toUid: 'y' }));
    await assertFails(setDoc(doc(db, 'reviews/r1'), { userId: 'x' }));
  });

  it('✗ inyección de XP arbitrario desde el cliente bloqueada', async () => {
    await seed('users/alice', {
      userId: 'alice', email: 'alice@test.com', username: 'Alice', xp: 100, completedGroupMatches: 0
    });
    const db = auth('alice', 'alice@test.com');
    // Intenta bajarse el XP (para un posible reset trampa)
    await assertFails(updateDoc(doc(db, 'users/alice'), { xp: 50 }));
    // Intenta ponerse XP muy alto
    await assertFails(updateDoc(doc(db, 'users/alice'), { xp: 99999999 }));
  });

  it('✗ no se puede crear review con campos de seguridad manipulados', async () => {
    const db = auth('bob', 'bob@test.com');
    // Intento de crear review con likeCount inflado y sin likedByIds correcto
    await assertFails(setDoc(doc(db, 'reviews/malicious'), {
      id: 'malicious', mediaId: 1, seasonNumber: 0,
      userId: 'bob', userEmail: 'bob@test.com', username: 'Bob',
      rating: 5, text: 'Test', hasSpoiler: false,
      likedByIds: ['uid1', 'uid2', 'uid3'],
      likeCount: 1000,  // likeCount no coincide con likedByIds.size()
      reportCount: 0, createdAt: now(), updatedAt: now()
    }));
  });
});
